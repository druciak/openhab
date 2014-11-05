/**
 * Copyright (c) 2010-2014, openHAB.org and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.openhab.binding.satel.internal.event;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * TODO document me!
 * 
 * @author Krzysztof Goworek
 * @since 1.7.0
 */
public class EventDispatcher {
	private static final Logger logger = LoggerFactory.getLogger(EventDispatcher.class);
	
	private final List<EventListener> eventListeners = new LinkedList<EventListener>();

	/**
	 * Add a listener for Satel events.
	 * @param eventListener the event listener to add.
	 */
	public void addEventListener(EventListener eventListener) {
		synchronized (this.eventListeners) {
			this.eventListeners.add(eventListener);
		}
	}

	/**
	 * Remove a listener for Satel events.
	 * @param eventListener the event listener to remove.
	 */
	public void removeEventListener(EventListener eventListener) {
		synchronized (this.eventListeners) {
			this.eventListeners.remove(eventListener);
		}
	}
	
	/**
	 * Dispatch incoming event to all listeners.
	 * @param event the event to distribute.
	 */
	public void dispatchEvent(SatelEvent event) {
		ArrayList<EventListener> listeners;
		synchronized (this.eventListeners) {
			listeners = new ArrayList<EventListener>(this.eventListeners);
		}
		logger.debug("Distributing event: {}", event);
		for (EventListener listener : listeners) {
			logger.trace("Distributing to {}", listener.toString());
			listener.incomingEvent(event);
		}
	}
}
