/**
 * Copyright (c) 2010-2014, openHAB.org and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.openhab.binding.satel.config;

import org.openhab.binding.satel.SatelBindingConfig;
import org.openhab.binding.satel.internal.event.IntegraStateEvent;
import org.openhab.binding.satel.internal.event.SatelEvent;
import org.openhab.binding.satel.internal.protocol.SatelMessage;
import org.openhab.binding.satel.internal.protocol.SatelModule.IntegraType;
import org.openhab.core.items.Item;
import org.openhab.core.types.Command;

/**
 * TODO document me!
 * 
 * @author Krzysztof Goworek
 * @since 1.7.0
 */
public class IntegraStateBindingConfig implements SatelBindingConfig {

	public enum ObjectType {
		input, zone, output, doors;
	}

	public interface StateType {
	}

	public enum InputState implements StateType {
		violation, tamper, alarm, tamper_alarm, alarm_memory, tamper_alarm_memory, bypass, no_violation_trouble, long_violation_trouble;
	}

	public enum ZoneState implements StateType {
		armed, really_armed, armed_mode_2, armed_mode_3, first_code_entered, entry_time, exit_time_gt_10, exit_time_lt_10, temporary_blocked, blocked_for_guard, alarm, fire_alarm, alarm_memory, fire_alarm_memory;
	}

	public enum DoorsState implements StateType {
		opened, opened_long;
	}

	private ObjectType objectType;
	private StateType stateType;
	private int objectNumber;

	public IntegraStateBindingConfig(String type, String config) {
		this.objectType = ObjectType.valueOf(type);
		
		String[] parts = config.split(":");
		switch (this.objectType) {
		case input:
			this.stateType = InputState.valueOf(parts[0]);
			break;
		case zone:
			this.stateType = ZoneState.valueOf(parts[0]);
			break;
		case output:
			break;
		case doors:
			this.stateType = DoorsState.valueOf(parts[0]);
			break;
		}
		if (parts.length > 1) {
			this.objectNumber = Integer.parseInt(parts[1]);
		}
	}

	@Override
	public void updateItem(Item item, SatelEvent event) {
		if (! (event instanceof IntegraStateEvent)) {
			return;
		}
		
		// TODO Auto-generated method stub
		
	}

	@Override
	public void receiveCommand(Command command) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public SatelMessage buildRefreshCommand(IntegraType integraType) {
		// TODO Auto-generated method stub
		return null;
	}
}
