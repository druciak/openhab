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

import org.openhab.binding.satel.internal.types.StateType;

/**
 * TODO document me!
 * 
 * @author Krzysztof Goworek
 * @since 1.7.0
 */
public class IntegraStateEvent implements SatelEvent {

	private StateType stateType;
	private BitSet stateBits;

	public IntegraStateEvent(StateType stateType, byte[] stateBits) {
		this.stateType = stateType;
		this.stateBits = BitSet.valueOf(stateBits);
	}

	public StateType getStateType() {
		return this.stateType;
	}

	public BitSet getStateBits() {
		return stateBits;
	}

	public boolean isSet(int nbr) {
		return stateBits.get(nbr);
	}

	public int statesSet() {
		return stateBits.cardinality();
	}
}
