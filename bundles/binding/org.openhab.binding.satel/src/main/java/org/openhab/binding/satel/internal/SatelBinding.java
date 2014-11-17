/**
 * Copyright (c) 2010-2014, openHAB.org and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.openhab.binding.satel.internal;

import java.util.ArrayList;
import java.util.Dictionary;
import java.util.List;

import org.apache.commons.lang.StringUtils;
import org.openhab.binding.satel.SatelBindingConfig;
import org.openhab.binding.satel.SatelBindingProvider;
import org.openhab.binding.satel.internal.event.EventListener;
import org.openhab.binding.satel.internal.event.NewStatesEvent;
import org.openhab.binding.satel.internal.event.SatelEvent;
import org.openhab.binding.satel.internal.protocol.Ethm1Module;
import org.openhab.binding.satel.internal.protocol.IntRSModule;
import org.openhab.binding.satel.internal.protocol.SatelMessage;
import org.openhab.binding.satel.internal.protocol.SatelModule;
import org.openhab.core.binding.AbstractActiveBinding;
import org.openhab.core.items.Item;
import org.openhab.core.types.Command;
import org.openhab.core.types.State;
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

	private long refreshInterval = 10000;
	private String userCode;
	private SatelModule satelModule = null;

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected String getName() {
		return "Satel Refresh Service";
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected long getRefreshInterval() {
		return refreshInterval;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void execute() {
		if (!isProperlyConfigured()) {
			logger.warn("Binding not properly configured, exiting");
			return;
		}

		if (!this.satelModule.isInitialized()) {
			logger.debug("Module not initialized yet, skipping refresh");
			return;
		}

		List<SatelMessage> commands = getRefreshCommands();
		logger.trace("Sending {} refresh commands", commands.size());
		for (SatelMessage message : commands) {
			this.satelModule.sendCommand(message);
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void updated(Dictionary<String, ?> config) throws ConfigurationException {
		logger.trace("Binding configuration updated: {}", config);

		if (config == null)
			return;

		this.refreshInterval = getLongValue(config, "refresh", 10000);
		this.userCode = (String) config.get("user_code");

		int timeout = getIntValue(config, "timeout", 5000);
		String host = (String) config.get("host");
		if (StringUtils.isNotBlank(host)) {
			this.satelModule = new Ethm1Module(host, getIntValue(config, "port", 7094), timeout,
					(String) config.get("encryption_key"));
		} else {
			this.satelModule = new IntRSModule((String) config.get("port"), timeout);
		}

		this.satelModule.addEventListener(this);
		this.satelModule.open();
		setProperlyConfigured(true);
		logger.trace("Binding properly configured");
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void internalReceiveCommand(String itemName, Command command) {
		if (!isProperlyConfigured()) {
			logger.warn("Binding not properly configured, exiting");
			return;
		}

		if (!this.satelModule.isInitialized()) {
			logger.debug("Module not initialized yet, ignoring command");
			return;
		}

		for (SatelBindingProvider provider : providers) {
			SatelBindingConfig itemConfig = provider.getItemConfig(itemName);
			if (itemConfig != null) {
				logger.trace("Sending internal command for item {}: {}", itemName, command);
				SatelMessage message = itemConfig.handleCommand(command, this.satelModule.getIntegraType(),
						this.userCode);
				if (message != null) {
					this.satelModule.sendCommand(message);
				}
				break;
			}
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void incomingEvent(SatelEvent event) {
		logger.trace("Handling incoming event: {}", event);

		// refresh all states that have changed
		if (event instanceof NewStatesEvent) {
			NewStatesEvent nse = (NewStatesEvent) event;
			List<SatelMessage> commands = getRefreshCommands();
			for (SatelMessage message : commands) {
				if (nse.isNew(message.getCommand())) {
					this.satelModule.sendCommand(message);
				}
			}
		}

		// update items
		for (SatelBindingProvider provider : providers) {
			for (String itemName : provider.getItemNames()) {
				SatelBindingConfig itemConfig = provider.getItemConfig(itemName);
				Item item = provider.getItem(itemName);
				State newState = itemConfig.convertEventToState(item, event);

				if (newState != null && !newState.equals(item.getState())) {
					eventPublisher.postUpdate(itemName, newState);
				}
			}
		}
	}

	/**
	 * Deactivates the binding by closing connected module.
	 */
	@Override
	public void deactivate() {
		if (this.satelModule != null) {
			this.satelModule.close();
			this.satelModule = null;
		}
	}

	private List<SatelMessage> getRefreshCommands() {
		logger.debug("Gathering refresh commands from all items");

		List<SatelMessage> commands = new ArrayList<SatelMessage>();
		for (SatelBindingProvider provider : providers) {
			for (String itemName : provider.getItemNames()) {
				logger.trace("Getting refresh command from item: {}", itemName);

				SatelBindingConfig itemConfig = provider.getItemConfig(itemName);
				SatelMessage message = itemConfig.buildRefreshMessage(this.satelModule.getIntegraType());

				if (message != null && !commands.contains(message)) {
					commands.add(message);
				}
			}
		}

		return commands;
	}

	private static int getIntValue(Dictionary<String, ?> config, String name, int defaultValue)
			throws ConfigurationException {
		String val = (String) config.get(name);
		try {
			if (StringUtils.isNotBlank(val)) {
				return Integer.parseInt(val);
			} else {
				return defaultValue;
			}
		} catch (Exception e) {
			throw new ConfigurationException(name, "invalid integer value");
		}
	}

	private static long getLongValue(Dictionary<String, ?> config, String name, long defaultValue)
			throws ConfigurationException {
		String val = (String) config.get(name);
		try {
			if (StringUtils.isNotBlank(val)) {
				return Long.parseLong(val);
			} else {
				return defaultValue;
			}
		} catch (Exception e) {
			throw new ConfigurationException(name, "invalid long value");
		}
	}
}
