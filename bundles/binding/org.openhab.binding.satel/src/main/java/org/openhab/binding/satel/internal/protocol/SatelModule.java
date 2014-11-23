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
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;

import org.openhab.binding.satel.internal.event.EventDispatcher;
import org.openhab.binding.satel.internal.event.EventListener;
import org.openhab.binding.satel.internal.event.IntegraVersionEvent;
import org.openhab.binding.satel.internal.event.SatelEvent;
import org.openhab.binding.satel.internal.protocol.command.ControlObjectCommand;
import org.openhab.binding.satel.internal.protocol.command.IntegraStateCommand;
import org.openhab.binding.satel.internal.protocol.command.IntegraVersionCommand;
import org.openhab.binding.satel.internal.protocol.command.NewStatesCommand;
import org.openhab.binding.satel.internal.protocol.command.SatelCommand;
import org.openhab.binding.satel.internal.types.ControlType;
import org.openhab.binding.satel.internal.types.DoorsState;
import org.openhab.binding.satel.internal.types.InputState;
import org.openhab.binding.satel.internal.types.IntegraType;
import org.openhab.binding.satel.internal.types.OutputControl;
import org.openhab.binding.satel.internal.types.OutputState;
import org.openhab.binding.satel.internal.types.StateType;
import org.openhab.binding.satel.internal.types.ZoneControl;
import org.openhab.binding.satel.internal.types.ZoneState;
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
	private static final byte FRAME_SYNC_ESC = (byte) 0xf0;
	private static final byte[] FRAME_START = { FRAME_SYNC, FRAME_SYNC };
	private static final byte[] FRAME_END = { FRAME_SYNC, (byte) 0x0d };
	private static final int QUEUE_SIZE = 10;

	private final Map<Byte, SatelCommand> supportedCommands = new ConcurrentHashMap<Byte, SatelCommand>();
	private final BlockingQueue<SatelMessage> sendQueue = new ArrayBlockingQueue<SatelMessage>(QUEUE_SIZE);

	private IntegraType integraType;
	private int timeout;
	private String integraVersion;
	private CommunicationChannel channel;
	private CommunicationThread communicationThread;

	protected interface CommunicationChannel {
		InputStream getInputStream() throws IOException;

		OutputStream getOutputStream() throws IOException;

		void disconnect();
	}

	public SatelModule(int timeout) {
		this.integraType = IntegraType.UNKNOWN;
		this.timeout = timeout;

		addEventListener(this);
		registerCommands();
	}

	public IntegraType getIntegraType() {
		return this.integraType;
	}

	public int getTimeout() {
		return this.timeout;
	}

	public boolean isConnected() {
		return this.channel != null;
	}

	public boolean isInitialized() {
		return this.integraType != IntegraType.UNKNOWN;
	}

	protected abstract CommunicationChannel connect();

	public synchronized void open() {
		this.communicationThread = new CommunicationThread();
		// get Integra version to properly initialize the module
		sendCommand(IntegraVersionCommand.buildMessage());
	}

	public synchronized void close() {
		if (this.communicationThread != null) {
			this.communicationThread.stopCommunication();
			this.communicationThread = null;
		}
		this.integraType = IntegraType.UNKNOWN;
	}

	public boolean sendCommand(SatelMessage cmd) {
		try {
			if (this.sendQueue.contains(cmd)) {
				logger.debug("Command already in the queue: {}", cmd);
			} else {
				this.sendQueue.put(cmd);
				logger.trace("Command enqueued: {}", cmd);
			}
			return true;
		} catch (InterruptedException e) {
			return false;
		}
	}

	@Override
	public void incomingEvent(SatelEvent event) {
		if (event instanceof IntegraVersionEvent) {
			IntegraVersionEvent versionEvent = (IntegraVersionEvent) event;
			this.integraType = IntegraType.valueOf(versionEvent.getType());
			this.integraVersion = versionEvent.getVersion();
			logger.info("Connection to {} initialized. Version: {}.", this.integraType.getName(), this.integraVersion);
		}
	}

	private void registerCommands() {
		this.supportedCommands.put(IntegraVersionCommand.COMMAND_CODE, new IntegraVersionCommand(this));
		this.supportedCommands.put(NewStatesCommand.COMMAND_CODE, new NewStatesCommand(this));
		for (StateType state : ZoneState.values()) {
			this.supportedCommands.put(state.getRefreshCommand(), new IntegraStateCommand(state, this));
		}
		for (StateType state : InputState.values()) {
			this.supportedCommands.put(state.getRefreshCommand(), new IntegraStateCommand(state, this));
		}
		for (StateType state : OutputState.values()) {
			this.supportedCommands.put(state.getRefreshCommand(), new IntegraStateCommand(state, this));
		}
		for (StateType state : DoorsState.values()) {
			this.supportedCommands.put(state.getRefreshCommand(), new IntegraStateCommand(state, this));
		}
		for (ControlType ct : ZoneControl.values()) {
			this.supportedCommands.put(ct.getControlCommand(), new ControlObjectCommand(ct, this));
		}
		for (ControlType ct : OutputControl.values()) {
			this.supportedCommands.put(ct.getControlCommand(), new ControlObjectCommand(ct, this));
		}
	}

	private SatelMessage readMessage() {
		try {
			InputStream is = this.channel.getInputStream();
			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			boolean inMessage = false;
			int syncBytes = 0;

			while (true) {
				byte b = (byte) is.read();

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
							if (b == FRAME_SYNC_ESC) {
								baos.write(FRAME_SYNC);
							} else if (b == FRAME_END[1]) {
								// end of message
								break;
							} else {
								logger.warn("Received invalid byte {}, discarding input: {}", String.format("%02X", b),
										baos.size());
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

				// if meanwhile thread has been interrupted, exit the loop
				if (Thread.interrupted()) {
					return null;
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
			logger.error("Unexpected exception occurred during writing a message", e);
		}

		return false;
	}

	private synchronized void disconnect() {
		this.sendQueue.clear();
		if (this.channel != null) {
			this.channel.disconnect();
			this.channel = null;
		}
		// if the module is not initialized yet
		// and communication thread is not stopped,
		// put initialization command in the queue
		// to schedule reconnect
		if (!this.isInitialized() && this.communicationThread.isAlive()) {
			sendCommand(IntegraVersionCommand.buildMessage());
		}
	}

	private boolean processNextCommand(TimeoutTimerTask timeoutTimerTask) {
		long reconnectionTime = 2 * this.timeout;

		try {
			SatelMessage message = this.sendQueue.take(), response = null;
			SatelCommand command = this.supportedCommands.get(message.getCommand());

			if (command == null) {
				logger.error("Unsupported command: {}", message);
				return true;
			}

			if (this.channel == null) {
				long connectStartTime = System.currentTimeMillis();
				synchronized (this) {
					this.channel = connect();
				}
				if (!this.isConnected()) {
					Thread.sleep(reconnectionTime - System.currentTimeMillis() + connectStartTime);
					return true;
				}
			}

			logger.debug("Sending message: {}", message);
			timeoutTimerTask.startCounting();
			boolean sent = writeMessage(message);

			if (sent) {
				logger.trace("Waiting for response");
				timeoutTimerTask.startCounting();
				response = this.readMessage();
				if (response != null) {
					logger.debug("Got response: {}", response);
					command.handleResponse(response);
				}
			}

			return sent && response != null;

		} catch (InterruptedException e) {
			// ignore
		} catch (Exception e) {
			// unexpected error, log and disconnect
			logger.info("Unhandled exception occurred in communication loop, disconnecting.", e);
		} finally {
			// disable timeout on exit
			timeoutTimerTask.stopCounting();
		}

		return false;
	}

	private class CommunicationThread extends Thread {
		private volatile boolean threadStopped;

		public CommunicationThread() {
			this.threadStopped = false;
			this.start();
		}

		@Override
		public void run() {
			logger.info("Communication thread started");

			// set timer to disconnect in case send
			// or receive operation takes too long
			Timer timeoutTimer = new Timer();
			TimeoutTimerTask timeoutTimerTask = new TimeoutTimerTask(this, timeout);
			timeoutTimer.schedule(timeoutTimerTask, 0, 200);

			while (!this.threadStopped) {
				
				if (!processNextCommand(timeoutTimerTask)) {
					// if something went wrong, disconnect
					disconnect();
				}
			}

			timeoutTimer.cancel();
			logger.info("Communication thread stopped");
		}

		void stopCommunication() {
			this.threadStopped = true;
			this.interrupt();
			try {
				this.join();
			} catch (InterruptedException e) {
				// ignore
			}
		}
	}

	private static class TimeoutTimerTask extends TimerTask {
		private Thread thread;
		private int timeout;
		private volatile long lastActivity = 0;

		TimeoutTimerTask(Thread thread, int timeout) {
			this.thread = thread;
			this.timeout = timeout;
		}

		public void startCounting() {
			this.lastActivity = System.currentTimeMillis();
		}

		public void stopCounting() {
			this.lastActivity = 0;
		}

		@Override
		public void run() {
			long timePassed = (this.lastActivity == 0) ? 0 : System.currentTimeMillis() - this.lastActivity;

			if (timePassed > this.timeout) {
				logger.debug("Send/receive timeout, disconnecting module.");
				stopCounting();
				this.thread.interrupt();
			}
		}
	}
}
