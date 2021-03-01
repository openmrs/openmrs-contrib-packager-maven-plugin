/**
 * This Source Code Form is subject to the terms of the Mozilla Public License,
 * v. 2.0. If a copy of the MPL was not distributed with this file, You can
 * obtain one at http://mozilla.org/MPL/2.0/. OpenMRS is also distributed under
 * the terms of the Healthcare Disclaimer located at http://openmrs.org/license.
 *
 * Copyright (C) OpenMRS Inc. OpenMRS is a registered trademark and the OpenMRS
 * graphic logo is a trademark of OpenMRS Inc.
 */
package org.openmrs.maven.plugins.packager;

import static org.twdata.maven.mojoexecutor.MojoExecutor.plugin;

import org.apache.maven.model.Plugin;

/**
 * Plugins in use throughout the project
 */
public class Plugins {

	public static final Plugin MAVEN_RESOURCES_PLUGIN =
			plugin("org.apache.maven.plugins", "maven-resources-plugin", "3.1.0");

	public static final Plugin MAVEN_DEPENDENCY_PLUGIN =
			plugin("org.apache.maven.plugins", "maven-dependency-plugin", "3.1.1");

	public static final Plugin MAVEN_ASSEMBLY_PLUGIN =
			plugin("org.apache.maven.plugins", "maven-assembly-plugin", "3.1.0");

}
