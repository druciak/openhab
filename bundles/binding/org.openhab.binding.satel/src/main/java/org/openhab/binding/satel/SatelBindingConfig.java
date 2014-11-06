/**
 * Copyright (c) 2010-2014, openHAB.org and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.openhab.binding.satel;

import org.openhab.binding.satel.internal.event.SatelEvent;
import org.openhab.binding.satel.internal.protocol.SatelMessage;
import org.openhab.binding.satel.internal.protocol.SatelModule.IntegraType;
import org.openhab.core.binding.BindingConfig;
import org.openhab.core.items.Item;
import org.openhab.core.types.Command;

/**
 * TODO document me!
 * 
 * @author Krzysztof Goworek
 * @since 1.7.0
 */
public interface SatelBindingConfig extends BindingConfig {
	
	/**
	 * @param item
	 * @param event
	 */
	void updateItem(Item item, SatelEvent event);
	
	/**
	 * @param command
	 */
	void receiveCommand(Command command);
	
	/**
	 * @param integraType
	 * @return
	 */
	SatelMessage buildRefreshCommand(IntegraType integraType);
}
