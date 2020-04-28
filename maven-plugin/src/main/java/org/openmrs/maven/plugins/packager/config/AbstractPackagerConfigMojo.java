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

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.Properties;

import org.apache.maven.plugin.MojoExecutionException;
import org.openmrs.maven.plugins.packager.AbstractPackagerMojo;

/**
 * Packages a standard set of OpenMRS configurations
 */
public abstract class AbstractPackagerConfigMojo extends AbstractPackagerMojo {

	/**
	 * Convenience method to get the project build directory
	 */
	protected File getPluginBuildDir() {
		File baseBuildDir = getBuildDir();
		File pluginBuildDir = new File(baseBuildDir, "openmrs-packager-config");
		if (!pluginBuildDir.exists()) {
			pluginBuildDir.mkdirs();
		}
		return pluginBuildDir;
	}

	/**
	 * @return the directory which will contain the resulting compiled configuration
	 */
	public File getCompiledConfigurationDir() {
		return new File(getPluginBuildDir(), "configuration");
	}

	/**
	 * Ensures the compiled configuration directory exists, by creating it if needed
	 */
	public void ensureCompiledConfigurationDir() {
		File configDir = getCompiledConfigurationDir();
		if (!configDir.exists()) {
			configDir.mkdirs();
		}
	}

	/**
	 * @return the file which will contain the resulting compiled constants
	 */
	public File getCompiledConstantsFile() {
		return new File(getCompiledConfigurationDir(), "constants.properties");
	}

	/**
	 * Convenience method to load properties from a file
	 */
	public Properties loadPropertiesFromFile(File file) throws MojoExecutionException {
		Properties p = new ConstantProperties();
		if (getCompiledConstantsFile().exists()) {
			try (FileInputStream in = new FileInputStream(getCompiledConstantsFile())) {
				p.load(in);
			}
			catch (Exception e) {
				throw new MojoExecutionException("Unable to load properties from file: " + file, e);
			}
		}
		return p;
	}

	/**
	 * Convenience method to write properties to a file
	 */
	public void savePropertiesToFile(Properties properties, File file) throws MojoExecutionException {
		try (FileOutputStream out = new FileOutputStream(file)) {
			properties.store(out, null);
		}
		catch (Exception e) {
			throw new MojoExecutionException("Unable to write properties to file: " + file, e);
		}
	}
}
