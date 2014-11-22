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

	@Override
	public String toString() {
		StringBuilder bitsStr = new StringBuilder();
		for (int i = this.stateBits.nextSetBit(0); i >= 0; i = this.stateBits.nextSetBit(i + 1)) {
			if (bitsStr.length() > 0) {
				bitsStr.append(",");
			}
			bitsStr.append(String.format("%02X", i+1));
		}
		return String.format("IntegraStateEvent: state = %s, changed = %s", stateType, bitsStr);
	}
}
