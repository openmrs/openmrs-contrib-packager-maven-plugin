/**
 * The contents of this file are subject to the OpenMRS Public License
 * Version 1.0 (the "License"); you may not use this file except in
 * compliance with the License. You may obtain a copy of the License at
 * http://license.openmrs.org
 *
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied. See the
 * License for the specific language governing rights and limitations
 * under the License.
 *
 * Copyright (C) OpenMRS, LLC.  All Rights Reserved.
 */

package org.openmrs.maven.plugins.packager.config;

import static org.twdata.maven.mojoexecutor.MojoExecutor.configuration;
import static org.twdata.maven.mojoexecutor.MojoExecutor.element;
import static org.twdata.maven.mojoexecutor.MojoExecutor.executeMojo;
import static org.twdata.maven.mojoexecutor.MojoExecutor.goal;
import static org.twdata.maven.mojoexecutor.MojoExecutor.plugin;

import java.io.File;
import java.util.List;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.io.FileUtils;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;

/**
 * The purpose of this Mojo is to pull in dependent artifacts that contain
 * configurations and copy these configurations into the resulting configuration artifact,
 * overwriting any that are already in place in the specified target folder
 */
@Mojo(name = "compile-configurations", defaultPhase = LifecyclePhase.COMPILE)
public class CompileConfigurationsMojo extends AbstractPackagerConfigMojo {

	// Configuration Directory
	@Parameter(property = "sourceDir", defaultValue = "configuration")
	private File sourceDir;

	// Dependency configurations that this project wishes to import from other projects
	@Parameter(property = "dependencyFile", defaultValue = "dependencies.yml")
	private File dependenciesFile;

	/**
	 * @throws MojoExecutionException if an error occurs
	 */
	public void execute() throws MojoExecutionException {
		addConfigurationDependencies();
		copyAndFilterConfiguration(sourceDir, getCompiledConfigurationDir());
		String openmrsServerId = System.getProperty("serverId");
		if (openmrsServerId != null) {
			copyConfigurationToLocalServer(openmrsServerId);
		}
	}

	/**
	 * This retrieves and unpacks and declared dependencies in the build directory
	 */
	protected void addConfigurationDependencies() throws MojoExecutionException {
		if (dependenciesFile != null && dependenciesFile.exists()) {
			getLog().info("Dependency configuration file found at: " + dependenciesFile);
			try {
				ObjectMapper m = getYamlMapper();
				List<ConfigDependency> configDependencies = m.readValue(dependenciesFile, new TypeReference<List<ConfigDependency>>(){});
				for (ConfigDependency d : configDependencies) {
					getLog().info("Retrieving and unpacking dependency: " + d);
					getLog().info("Adding dependency: " + d);
					File unpackDir = new File(getBuildDir(), "dependencies/" + d.toString("_"));
					unpackDependency(d, unpackDir);
					copyAndFilterConfiguration(unpackDir, getCompiledConfigurationDir());
				}
			}
			catch (Exception e) {
				throw new MojoExecutionException("Unable to read dependency configurations from " + dependenciesFile, e);
			}
		}
		else {
			getLog().info("No dependency configuration file found at " + dependenciesFile);
		}
	}

	/**
	 * @return the file that contains the compiled configuration artifacts
	 */
	public File getCompiledConfigurationDir() {
		return new File(getBuildDir(), "configuration");
	}

	/**
	 * Executes the maven dependency plugin, unpacking dependent artifacts into a standard directory structure
	 * in the build directory so that these can be pulled in as a appropriate to the final configurations
	 */
	public void unpackDependency(ConfigDependency d, File unpackDir) throws MojoExecutionException {

		getLog().info("Unpacking dependency to " + unpackDir);
		executeMojo(
				plugin("org.apache.maven.plugins", "maven-dependency-plugin", "3.1.1"),
				goal("unpack"),
				configuration(
						element("artifactItems",
								element("artifactItem",
										element("groupId", d.getGroupId()),
										element("artifactId", d.getArtifactId()),
										element("version", d.getVersion()),
										element("type", "zip"),
										element("overWrite", "true"),
										element("outputDirectory", unpackDir.getAbsolutePath())
								)
						)
				),
				getMavenExecutionEnvironment()
		);
	}

	/**
	 * Executes the maven resources plugin, copying configuration resources
	 * from one directory to another, and ensuring the resource filtering is enabled
	 * so that variable replacements can take place.
	 */
	protected void copyAndFilterConfiguration(File fromDir, File toDir) throws MojoExecutionException {
		getLog().info("Adding and filtering resources from " + fromDir + " to " + toDir);
		executeMojo(
				plugin("org.apache.maven.plugins", "maven-resources-plugin", "3.1.0"),
				goal("copy-resources"),
				configuration(
						element("outputDirectory", toDir.getAbsolutePath()),
						element("encoding", "UTF-8"),
						element("resources",
								element("resource",
										element("directory", fromDir.getAbsolutePath()),
										element("filtering", "true")
								)
						)
				),
				getMavenExecutionEnvironment()
		);
	}

	/**
	 * This copies the configuration from the compil
	 * @param serverId
	 */
	protected void copyConfigurationToLocalServer(String serverId) throws MojoExecutionException {
		getLog().info("Copying configuration to SDK server : " + serverId);
		File sdkHome = new File(System.getProperty("user.home"), "openmrs");
		if (!sdkHome.exists()) {
			throw new MojoExecutionException("No SDK directory found at " + sdkHome);
		}
		File serverHome = new File(sdkHome, serverId);
		if (!serverHome.exists()) {
			throw new MojoExecutionException("No server directory found at " + serverHome);
		}
		File configurationDir = new File(serverHome, "configuration");
		if (!configurationDir.exists()) {
			getLog().info("No current configuration directory exists, creating " + configurationDir);
		}
		else {
			getLog().warn("Configuration directory already exists, deleting and recreating " + configurationDir);
			deleteDirectory(configurationDir);
		}
		configurationDir.mkdir();
		copyAndFilterConfiguration(getCompiledConfigurationDir(), configurationDir);
		getLog().warn("Configuration copied into: " + configurationDir);
	}

	/**
	 * Utility method to forcibly delete a directory recursively, including files within
	 */
	private void deleteDirectory(File dir) throws MojoExecutionException {
		try {
			FileUtils.deleteDirectory(dir);
		}
		catch (Exception e) {
			throw new MojoExecutionException("Unale to delete directory: " + dir, e);
		}
	}
}