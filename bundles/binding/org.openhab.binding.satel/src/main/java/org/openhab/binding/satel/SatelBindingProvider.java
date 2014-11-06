/**
 * Copyright (c) 2010-2014, openHAB.org and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.openhab.binding.satel;

import org.openhab.core.binding.BindingProvider;
import org.openhab.core.items.Item;

/**
 * TODO document me!
 * 
 * @author Krzysztof Goworek
 * @since 1.6.0
 */
public interface SatelBindingProvider extends BindingProvider {
	
	/**
	 * Returns the {@link Item} with the specified item name. Returns null
	 * if the item was not found.
	 * @param itemName the name of the item.
	 * @return the item.
	 */
	Item getItem(String itemName);
	
	/**
	 * @param itemName
	 * @return
	 */
	SatelBindingConfig getItemConfig(String itemName);
}
