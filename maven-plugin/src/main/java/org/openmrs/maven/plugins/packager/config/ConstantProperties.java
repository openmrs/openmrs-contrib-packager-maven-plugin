/**
 * This Source Code Form is subject to the terms of the Mozilla Public License,
 * v. 2.0. If a copy of the MPL was not distributed with this file, You can
 * obtain one at http://mozilla.org/MPL/2.0/. OpenMRS is also distributed under
 * the terms of the Healthcare Disclaimer located at http://openmrs.org/license.
 *
 * Copyright (C) OpenMRS Inc. OpenMRS is a registered trademark and the OpenMRS
 * graphic logo is a trademark of OpenMRS Inc.
 */
package org.openmrs.maven.plugins.packager.config;

import java.util.Collections;
import java.util.Enumeration;
import java.util.Properties;
import java.util.Set;
import java.util.TreeSet;

/**
 * Extends a properties file to better control behavior to improve usability.
 * Primary initial use case is to ensure properties are saved to file in a predictable and user-friendly order
 */
public class ConstantProperties extends Properties {

	public ConstantProperties() {
		super();
	}

	@Override
	public synchronized Enumeration<Object> keys() {
		Enumeration<Object> parentKeys = super.keys();
		Set<Object> sortedKeys = new TreeSet<>();
		while (parentKeys.hasMoreElements()) {
			sortedKeys.add(parentKeys.nextElement());
		}
		return Collections.enumeration(sortedKeys);
	}
}
