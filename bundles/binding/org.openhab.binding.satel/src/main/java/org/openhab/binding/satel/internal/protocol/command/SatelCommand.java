/**
 * Copyright (c) 2010-2014, openHAB.org and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.openhab.binding.satel.internal.protocol.command;

import org.openhab.binding.satel.internal.event.EventDispatcher;
import org.openhab.binding.satel.internal.protocol.SatelMessage;

/**
 * TODO document me!
 * 
 * @author Krzysztof Goworek
 * @since 1.7.0
 */
public abstract class SatelCommand {
	private EventDispatcher eventDispatcher;
	
	public SatelCommand(EventDispatcher eventDispatcher) {
		this.eventDispatcher = eventDispatcher;
	}
	
	protected EventDispatcher getEventDispatcher() {
		return this.eventDispatcher;
	}
	
	public abstract void handleResponse(SatelMessage response);
}
