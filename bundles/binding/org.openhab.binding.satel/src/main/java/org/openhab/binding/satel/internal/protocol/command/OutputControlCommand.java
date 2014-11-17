/**
 * Copyright (c) 2010-2014, openHAB.org and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.openhab.binding.satel.internal.protocol.command;

import java.util.BitSet;

import org.openhab.binding.satel.internal.event.EventDispatcher;
import org.openhab.binding.satel.internal.event.NewStatesEvent;
import org.openhab.binding.satel.internal.protocol.SatelMessage;
import org.openhab.binding.satel.internal.types.OutputControl;
import org.openhab.binding.satel.internal.types.OutputState;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * TODO document me!
 * 
 * @author Krzysztof Goworek
 * @since 1.7.0
 */
public class OutputControlCommand extends SatelCommand {
	private static final Logger logger = LoggerFactory.getLogger(OutputControlCommand.class);

	private OutputControl outputControl;

	public OutputControlCommand(OutputControl outputControl, EventDispatcher eventDispatcher) {
		super(eventDispatcher);
		this.outputControl = outputControl;
	}

	public static SatelMessage buildMessage(OutputControl outputControl, BitSet outputs) {
		return new SatelMessage(outputControl.getRefreshCommand(), outputs.toByteArray());
	}

	@Override
	public void handleResponse(SatelMessage response) {
		// validate response
		if (response.getCommand() != this.outputControl.getRefreshCommand()) {
			logger.error("Invalid response code: {}", response.getCommand());
			return;
		}
		// force outputs refresh
		BitSet newStates = new BitSet(48);
		newStates.set(OutputState.state.getRefreshCommand());
		this.getEventDispatcher().dispatchEvent(new NewStatesEvent(newStates));
	}
}
