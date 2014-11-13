/**
 * Copyright (c) 2010-2014, openHAB.org and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.openhab.binding.satel.internal;

import java.util.Set;

import org.openhab.binding.satel.SatelBindingConfig;
import org.openhab.binding.satel.SatelBindingProvider;
import org.openhab.binding.satel.config.IntegraStateBindingConfig;
import org.openhab.core.binding.BindingConfig;
import org.openhab.core.items.Item;
import org.openhab.core.library.items.ContactItem;
import org.openhab.core.library.items.NumberItem;
import org.openhab.core.library.items.SwitchItem;
import org.openhab.model.item.binding.AbstractGenericBindingProvider;
import org.openhab.model.item.binding.BindingConfigParseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * TODO document me!
 * 
 * @author Krzysztof Goworek
 * @since 1.7.0
 */
public class SatelGenericBindingProvider extends AbstractGenericBindingProvider implements SatelBindingProvider {

	private static final Logger logger = LoggerFactory.getLogger(SatelGenericBindingProvider.class);
	
	/**
	 * {@inheritDoc}
	 */
	@Override
	public String getBindingType() {
		return "satel";
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void validateItemType(Item item, String bindingConfig) throws BindingConfigParseException {
		// TODO implement me!
		if ((item instanceof NumberItem) || (item instanceof ContactItem) || (item instanceof SwitchItem)) {
			return;
		}
		throw new BindingConfigParseException("item '" + item.getName()
			+ "' is of type '" + item.getClass().getSimpleName()
			+ "', only Number- Contact- and Switch type is allowed - please check your *.items configuration");
	}
	
	/**
	 * {@inheritDoc}
	 */
	@Override
	public void processBindingConfiguration(String context, Item item, String bindingConfig) throws BindingConfigParseException {
		logger.trace("Processing binding configuration for item {}", item.getName());
		
		super.processBindingConfiguration(context, item, bindingConfig);
		
		String[] parts = bindingConfig.split(":", 2);		
		addBindingConfig(item, this.createBindingConfig(parts[0], (parts.length > 1) ? parts[1] : ""));
	}
	
	@Override
	public Item getItem(String itemName) {
		for (Set<Item> items : contextMap.values()) {
			if (items != null) {
				for (Item item : items) {
					if (itemName.equals(item.getName())) {
						return item;
					}
				}
			}
		}
		return null;
	}	
	
	/**
	 * {@inheritDoc}
	 */
	@Override
	public SatelBindingConfig getItemConfig(String itemName) {
		return (SatelBindingConfig) this.bindingConfigs.get(itemName);
	}

	private BindingConfig createBindingConfig(String type, String config) throws BindingConfigParseException {
		try {
			// try IntegraStateBindingConfig first
			for (IntegraStateBindingConfig.ObjectType t : IntegraStateBindingConfig.ObjectType.values())
				if (t.name().equals(type))
					return new IntegraStateBindingConfig(type, config);
			
			// no more options, throw parse exception
			throw new BindingConfigParseException(String.format("Invalid binding configuration type: {}", type));
			
		} catch (Exception e) {
			throw new BindingConfigParseException(String.format("Invalid binding configuration: {}:{}", type, config));
		}
	}
}
