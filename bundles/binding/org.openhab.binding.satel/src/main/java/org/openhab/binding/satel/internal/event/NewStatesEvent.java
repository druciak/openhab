/**
 * Copyright (c) 2010-2014, openHAB.org and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.openhab.binding.satel.internal.event;

import java.util.BitSet;

/**
 * TODO document me!
 * 
 * @author Krzysztof Goworek
 * @since 1.7.0
 */
public class NewStatesEvent implements SatelEvent {

	private BitSet newStates;

	public NewStatesEvent(BitSet newStates) {
		this.newStates = newStates;
	}

	public NewStatesEvent(byte[] newStates) {
		this(BitSet.valueOf(newStates));
	}

	public boolean isNew(int nbr) {
		return newStates.get(nbr);
	}
}
