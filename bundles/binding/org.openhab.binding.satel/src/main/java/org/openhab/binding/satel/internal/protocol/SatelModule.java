/**
 * Copyright (c) 2010-2014, openHAB.org and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.openhab.binding.satel.internal.protocol;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Map;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;

import org.openhab.binding.satel.internal.event.EventDispatcher;
import org.openhab.binding.satel.internal.event.EventListener;
import org.openhab.binding.satel.internal.event.IntegraVersionEvent;
import org.openhab.binding.satel.internal.event.SatelEvent;
import org.openhab.binding.satel.internal.protocol.command.IntegraVersionCommand;
import org.openhab.binding.satel.internal.protocol.command.SatelCommand;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * TODO document me!
 * 
 * @author Krzysztof Goworek
 * @since 1.7.0
 */
public abstract class SatelModule extends EventDispatcher implements EventListener {
	private static final Logger logger = LoggerFactory.getLogger(SatelModule.class);

	private static final byte FRAME_SYNC = (byte) 0xfe;
	private static final byte[] FRAME_START = { FRAME_SYNC, FRAME_SYNC };
	private static final byte[] FRAME_END = { FRAME_SYNC, (byte) 0x0d };
	private static final int QUEUE_SIZE = 10; 
	
	private final Map<Byte, SatelCommand> supportedCommands = new ConcurrentHashMap<Byte, SatelCommand>();
	private final BlockingQueue<SatelMessage> sendQueue = new ArrayBlockingQueue<SatelMessage>(QUEUE_SIZE);

	private IntegraType integraType;
	private String integraVersion;
	private CommunicationChannel channel;
	private Thread communicationThread;
	
	public enum IntegraType {
		UNKNOWN(-1, "Unknown"),
		I24(0, "Integra 24"), 
		I32(1, "Integra 32"), 
		I64(2, "Integra 64"), 
		I128(3, "Integra 128"), 
		I128_SIM300(4, "Integra 128-WRL SIM300"), 
		I128_LEON(132, "Integra 128-WRL LEON"),
		I64_PLUS(66, "Integra 64 Plus"),
		I128_PLUS(67, "Integra 128 Plus"),
		I256_PLUS(72, "Integra 256 Plus");
		
		private int code;
		private String name;
		
		IntegraType(int code, String name) {
			this.code = code;
			this.name = name;
		}
		
		int getCode() {
			return this.code;
		}
		
		String getName() {
			return this.name;
		}
		
		static IntegraType valueOf(int code) {
			for (IntegraType val : IntegraType.values()) {
				if (val.getCode() == code)
					return val;
			}
			return UNKNOWN;
		}
	}
	
	protected interface CommunicationChannel {
		InputStream getInputStream() throws IOException;
		OutputStream getOutputStream() throws IOException;
		void disconnect();
	}

	public SatelModule() {
		this.integraType = IntegraType.UNKNOWN;
		
		addEventListener(this);
		registerCommands();
	}
	
	public IntegraType getIntegraType() {
		return this.integraType;
	}

	public boolean isConnected() {
		return this.channel != null;
	}

	public boolean isInitialized() {
		return this.integraType != IntegraType.UNKNOWN;
	}

	protected abstract CommunicationChannel connect();

	public synchronized void open() {
		this.channel = connect();
		if (this.channel == null) {
			close();
			return;
		}
		this.communicationThread = new Thread() {
			@Override
			public void run() {
				logger.info("Communication thread started");
				communicationLoop();
				logger.info("Communication thread stopped");
			}
		};
		this.communicationThread.start();
		
		sendCommand(IntegraVersionCommand.buildMessage());
	}

	public synchronized void close() {
		if (this.communicationThread != null) {
			this.communicationThread.interrupt();
			try {
				this.communicationThread.join();
			} catch (InterruptedException e) {
			}
			this.communicationThread = null;
		}
		if (this.channel != null) {
			this.channel.disconnect();
			this.channel = null;
		}
		this.sendQueue.clear();
		this.integraType = IntegraType.UNKNOWN;
	}
	
	public boolean sendCommand(SatelMessage cmd) {
		try {
			this.sendQueue.put(cmd);
			return true;
		} catch (InterruptedException e) {
			return false;
		}
	}
	
	public void incomingEvent(SatelEvent event) {
		if (event instanceof IntegraVersionEvent) {
			IntegraVersionEvent versionEvent = (IntegraVersionEvent) event;
			this.integraType = IntegraType.valueOf(versionEvent.getType());
			this.integraVersion = versionEvent.getVersion();
			logger.info("Connection to {} initialized. Version: {}.", this.integraType.getName(), this.integraVersion);
		}
	}
	
	private void registerCommands() {
		// TODO add other commands
		this.supportedCommands.put(IntegraVersionCommand.COMMAND_CODE, new IntegraVersionCommand(this));
	}

	private SatelMessage readMessage() {
		try {
			InputStream is = this.channel.getInputStream();
			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			boolean inMessage = false;
			int syncBytes = 0;

			while (true) {
				int b = is.read();

				if (b == FRAME_SYNC) {
					if (inMessage) {
						if (syncBytes == 0) {
							// special sequence or end of message
							// wait for next byte
						} else {
							logger.warn("Received frame sync bytes, discarding input: {}", baos.size());
							// clear gathered bytes, we wait for new message
							inMessage = false;
							baos.reset();
						}
					}
					++syncBytes;
				} else {
					if (inMessage) {
						if (syncBytes == 0) {
							// in sync, we have next message byte
							baos.write(b);
						} else if (syncBytes == 1) {
							if (b == 0xf0) {
								baos.write(FRAME_SYNC);
							} else if (b == FRAME_END[1]) {
								// end of message
								break;
							} else {
								logger.warn("Received invalid byte, discarding input: {}", baos.size());
								// clear gathered bytes, we have new message
								inMessage = false;
								baos.reset();
							}
						} else {
							logger.error("Sync bytes in message: {}", syncBytes);
						}
					} else if (syncBytes >= 2) {
						// synced, we have first message byte
						inMessage = true;
						baos.write(b);
					} else {
						// discard all bytes until synced
					}
					syncBytes = 0;
				}
			}

			// return read message
			return SatelMessage.fromBytes(baos.toByteArray());

		} catch (IOException e) {
			logger.error("Unexpected exception occurred during reading a message", e);
		}

		return null;
	}

	private boolean writeMessage(SatelMessage message) {
		try {
			OutputStream os = this.channel.getOutputStream();
			
			os.write(FRAME_START);
			for (byte b : message.getBytes()) {
				os.write(b);
				if (b == FRAME_SYNC) {
					os.write(0xf0);
				}
			}
			os.write(FRAME_END);
			os.flush();
			return true;

		} catch (IOException e) {
			logger.error("Unexpected exception occurred during sending a message", e);
		}
		return false;
	}

	private void communicationLoop() {
		while (! Thread.interrupted()) {
			try {
				SatelMessage message = this.sendQueue.take(), response = null;
				SatelCommand command = this.supportedCommands.get(message.getCommand());

				if (command == null) {
					logger.error("Unsupported command: {}", message);
					continue;
				}

				logger.debug("Sending message: {}", message);
				boolean sent = writeMessage(message);
				
				if (sent) {
					logger.trace("Waiting for response");
					response = this.readMessage();
					if (response != null) {
						logger.debug("Got response: {}", response);
						command.handleResponse(response);
					}
				}
			} catch (InterruptedException e) {
				// exit thread
			} catch (Exception e) {
				logger.info("Unhandled exception occurred in communication loop", e);
			}
		}
	}
}
