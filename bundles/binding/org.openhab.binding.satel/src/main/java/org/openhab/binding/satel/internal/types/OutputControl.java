/**
 * Copyright (c) 2010-2014, openHAB.org and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.openhab.binding.satel.internal.types;

/**
 * TODO document me!
 * 
 * @author Krzysztof Goworek
 * @since 1.7.0
 */
public enum OutputControl implements StateType {
	on(0x88), off(0x89), toggle(0x91);

	private byte refreshCommand;

	OutputControl(int refreshCommand) {
		this.refreshCommand = (byte) refreshCommand;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public byte getRefreshCommand() {
		return refreshCommand;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public ObjectType getObjectType() {
		return ObjectType.output;
	}
}
