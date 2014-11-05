/**
 * Copyright (c) 2010-2014, openHAB.org and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.openhab.binding.satel.internal;

import java.util.Dictionary;

import org.apache.commons.lang.StringUtils;
import org.openhab.binding.satel.SatelBindingProvider;
import org.openhab.binding.satel.internal.event.EventListener;
import org.openhab.binding.satel.internal.event.IntegraStateEvent;
import org.openhab.binding.satel.internal.event.SatelEvent;
import org.openhab.binding.satel.internal.protocol.Ethm1Module;
import org.openhab.binding.satel.internal.protocol.IntRSModule;
import org.openhab.binding.satel.internal.protocol.SatelModule;
import org.openhab.core.binding.AbstractActiveBinding;
import org.openhab.core.types.Command;
import org.osgi.service.cm.ConfigurationException;
import org.osgi.service.cm.ManagedService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * TODO document me!
 * 
 * @author Krzysztof Goworek
 * @since 1.7.0
 */
public class SatelBinding extends AbstractActiveBinding<SatelBindingProvider> implements ManagedService, EventListener {
	
	private static final Logger logger = LoggerFactory.getLogger(SatelBinding.class);

	/**
	 * the refresh interval which is used to poll values from the OneWire server
	 * (optional, defaults to 10000s)
	 */
	private long refreshInterval = 10000;

	private SatelModule satelModule = null;

	@Override
	protected String getName() {
		return "Satel Refresh Service";
	}

	@Override
	protected long getRefreshInterval() {
		return refreshInterval;
	}

	/**
	 * @{inheritDoc}
	 */
	@Override
	public void execute() {
		// TODO implement me!
		if (! isProperlyConfigured()) {
			logger.warn("Binding not properly configured, exiting");
			return;
		}
		
		for (SatelBindingProvider provider : providers) {
			// TODO: implement me!
		}
	}
	
	/**
	 * {@inheritDoc}
	 */
	@Override
	public void updated(Dictionary<String, ?> config) throws ConfigurationException {
		if (config == null)
			return;
		
		String refreshIntervalString = (String) config.get("refresh");
		if (StringUtils.isNotBlank(refreshIntervalString)) {
			this.refreshInterval = Long.parseLong(refreshIntervalString);
		}

		int timeout;
		try {
			String timeoutString = (String) config.get("timeout");
			if (StringUtils.isNotBlank(timeoutString))
				timeout = Integer.parseInt(timeoutString);
			else
				timeout = 5000;
		} catch (Exception e) {
			throw new ConfigurationException("timeout", "invalid value");
		}
		
		String host = (String) config.get("host");
		if (StringUtils.isNotBlank(host)) {
			int port;
			try {
				port = Integer.parseInt((String) config.get("port"));
			} catch (Exception e) {
				throw new ConfigurationException("port", "invalid value");
			}
			// TODO implement encryption
			this.satelModule = new Ethm1Module(host, port, timeout);
		} else {
			this.satelModule = new IntRSModule((String) config.get("port"), timeout);
		}
		
		this.satelModule.addEventListener(this);
		this.satelModule.open();
		setProperlyConfigured(true);
	}
	
	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void internalReceiveCommand(String itemName, Command command) {
		if (! isProperlyConfigured()) {
			logger.warn("Binding not properly configured, exiting");
			return;
		}
		
		for (SatelBindingProvider provider : providers) {
			// TODO: implement me!
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void incomingEvent(SatelEvent event) {
		if (event instanceof IntegraStateEvent) {
			// TODO handle event
		}
	}
	
	/**
	 * Deactivates the binding. The Controller is stopped and the serial interface
	 * is closed as well.
	 */
	@Override
	public void deactivate() {
		if (this.satelModule != null) {
			this.satelModule.close();
			this.satelModule = null;
		}
	}
}
