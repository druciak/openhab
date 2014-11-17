/**
 * Copyright (c) 2010-2014, openHAB.org and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.openhab.binding.satel.config;

import java.util.BitSet;

import org.openhab.binding.satel.SatelBindingConfig;
import org.openhab.binding.satel.internal.event.IntegraStateEvent;
import org.openhab.binding.satel.internal.event.SatelEvent;
import org.openhab.binding.satel.internal.protocol.SatelMessage;
import org.openhab.binding.satel.internal.protocol.SatelModule.IntegraType;
import org.openhab.binding.satel.internal.protocol.command.IntegraStateCommand;
import org.openhab.binding.satel.internal.protocol.command.OutputControlCommand;
import org.openhab.binding.satel.internal.types.DoorsState;
import org.openhab.binding.satel.internal.types.InputState;
import org.openhab.binding.satel.internal.types.ObjectType;
import org.openhab.binding.satel.internal.types.OutputControl;
import org.openhab.binding.satel.internal.types.OutputState;
import org.openhab.binding.satel.internal.types.StateType;
import org.openhab.binding.satel.internal.types.ZoneState;
import org.openhab.core.items.Item;
import org.openhab.core.library.items.ContactItem;
import org.openhab.core.library.items.NumberItem;
import org.openhab.core.library.items.SwitchItem;
import org.openhab.core.library.types.DecimalType;
import org.openhab.core.library.types.OnOffType;
import org.openhab.core.library.types.OpenClosedType;
import org.openhab.core.types.Command;
import org.openhab.core.types.State;
import org.openhab.model.item.binding.BindingConfigParseException;

/**
 * TODO document me!
 * 
 * @author Krzysztof Goworek
 * @since 1.7.0
 */
public class IntegraStateBindingConfig implements SatelBindingConfig {

	private final static DecimalType DECIMAL_ONE = new DecimalType(1);

	private StateType stateType;
	private int objectNumber;

	private IntegraStateBindingConfig(StateType stateType, int objectNumber) {
		this.stateType = stateType;
		this.objectNumber = objectNumber;
	}

	/**
	 * Parses given binding configuration and creates configuration object.
	 * 
	 * @param bindingConfig
	 *            config to parse
	 * @return parsed config object or <code>null</code> if config does not
	 *         match
	 * @throws BindingConfigParseException
	 *             in case of parse errors
	 */
	public static IntegraStateBindingConfig parseConfig(String bindingConfig) throws BindingConfigParseException {
		String[] configElements = bindingConfig.split(":");
		int idx = 0;
		ObjectType objectType;

		try {
			objectType = ObjectType.valueOf(configElements[idx++]);
		} catch (Exception e) {
			// wrong config type, skip parsing
			return null;
		}

		StateType stateType = null;
		int objectNumber = -1;

		try {
			switch (objectType) {
			case input:
				stateType = InputState.valueOf(configElements[idx++]);
				break;
			case zone:
				stateType = ZoneState.valueOf(configElements[idx++]);
				break;
			case output:
				stateType = OutputState.state;
				break;
			case doors:
				stateType = DoorsState.valueOf(configElements[idx++]);
				break;
			}
		} catch (Exception e) {
			throw new BindingConfigParseException(String.format("Invalid state type: {}", bindingConfig));
		}

		if (idx < configElements.length) {
			try {
				objectNumber = Integer.parseInt(configElements[idx++]);
			} catch (NumberFormatException e) {
				throw new BindingConfigParseException(String.format("Invalid object number: {}", bindingConfig));
			}
		}

		if (idx < configElements.length) {
			// if anything left, throw exception
			throw new BindingConfigParseException(String.format("Too many elements: {}", bindingConfig));
		}

		return new IntegraStateBindingConfig(stateType, objectNumber);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public State convertEventToState(Item item, SatelEvent event) {
		if (!(event instanceof IntegraStateEvent)) {
			return null;
		}

		IntegraStateEvent stateEvent = (IntegraStateEvent) event;
		if (stateEvent.getStateType() != this.stateType) {
			return null;
		}

		if (this.objectNumber >= 0) {
			if (item instanceof ContactItem) {
				return stateEvent.isSet(this.objectNumber) ? OpenClosedType.OPEN : OpenClosedType.CLOSED;
			} else if (item instanceof SwitchItem) {
				return stateEvent.isSet(this.objectNumber) ? OnOffType.ON : OnOffType.OFF;
			} else if (item instanceof NumberItem) {
				return stateEvent.isSet(this.objectNumber) ? DECIMAL_ONE : DecimalType.ZERO;
			}
		} else {
			if (item instanceof ContactItem) {
				return (stateEvent.statesSet() > 0) ? OpenClosedType.OPEN : OpenClosedType.CLOSED;
			} else if (item instanceof SwitchItem) {
				return (stateEvent.statesSet() > 0) ? OnOffType.ON : OnOffType.OFF;
			} else if (item instanceof NumberItem) {
				return new DecimalType(stateEvent.statesSet());
			}
		}

		return null;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public SatelMessage handleCommand(Command command, IntegraType integraType, String userCode) {
		if (this.stateType.getObjectType() == ObjectType.output && command instanceof OnOffType
				&& this.objectNumber >= 0) {

			BitSet outputs = new BitSet((integraType == IntegraType.I256_PLUS) ? 256 : 128);
			outputs.set(this.objectNumber);

			if ((OnOffType) command == OnOffType.ON) {
				return OutputControlCommand.buildMessage(OutputControl.on, outputs);
			} else {
				return OutputControlCommand.buildMessage(OutputControl.off, outputs);
			}
		}

		return null;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public SatelMessage buildRefreshMessage(IntegraType integraType) {
		return IntegraStateCommand.buildMessage(this.stateType, integraType == IntegraType.I256_PLUS);
	}
}
