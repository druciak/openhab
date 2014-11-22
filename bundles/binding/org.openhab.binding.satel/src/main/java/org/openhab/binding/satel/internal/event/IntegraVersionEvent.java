/**
 * Copyright (c) 2010-2014, openHAB.org and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.openhab.binding.satel.internal.event;

/**
 * TODO document me!
 * 
 * @author Krzysztof Goworek
 * @since 1.7.0
 */
public class IntegraVersionEvent implements SatelEvent {

	private byte type;
	private String version;
	private byte language;
	private boolean settingsInFlash;

	public IntegraVersionEvent(byte type, String version, byte language, boolean settingsInFlash) {
		this.type = type;
		this.version = version;
		this.language = language;
		this.settingsInFlash = settingsInFlash;
	}

	public byte getType() {
		return type;
	}

	public String getVersion() {
		return version;
	}

	public byte getLanguage() {
		return language;
	}

	public boolean getSettingsInflash() {
		return this.settingsInFlash;
	}

	@Override
	public String toString() {
		return String.format("IntegraVersionEvent: type = %d, version = %s, language = %d, settingsInFlash = %b",
				this.type, this.version, this.language, this.settingsInFlash);
	}
}
