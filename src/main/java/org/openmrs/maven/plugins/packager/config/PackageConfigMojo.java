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
import static org.twdata.maven.mojoexecutor.MojoExecutor.executionEnvironment;
import static org.twdata.maven.mojoexecutor.MojoExecutor.goal;
import static org.twdata.maven.mojoexecutor.MojoExecutor.plugin;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.ValueNode;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import org.apache.commons.io.IOUtils;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.BuildPluginManager;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;

/**
 * Packages a standard set of OpenMRS configurations
 */
@Mojo(name = "package-config", defaultPhase = LifecyclePhase.GENERATE_RESOURCES)
public class PackageConfigMojo extends AbstractMojo {

	@Component
	private MavenProject mavenProject;

	@Component
	private MavenSession mavenSession;

	@Component
	private BuildPluginManager pluginManager;

	// Configuration Directory
	@Parameter(property = "configDir", defaultValue = "configuration")
	private File configDir;

	// Constants that can be used as variable replacements
	@Parameter(property = "constantFile", defaultValue = "constants.yml")
	private File constantFile;

	// Dependency configurations that this project wishes to import from other projects
	@Parameter(property = "dependencyFile", defaultValue = "dependencies.yml")
	private File dependenciesFile;

	/**
	 * Executes the validate goal
	 * @throws MojoExecutionException if an error occurs
	 */
	public void execute() throws MojoExecutionException, MojoFailureException {
		setupResourceFilters();
		addConfigurationDependencies();
		copyAndFilterConfiguration(configDir, getPackageDir());
		createArchive();
	}

	public void setupResourceFilters() throws MojoExecutionException {
		Properties toStore = new Properties();
		try {
			if (constantFile != null && constantFile.exists()) {
				getLog().info("Constant file found at: " + constantFile);

				if (constantFile.getAbsolutePath().endsWith(".properties")) {
					toStore.load(new FileInputStream(constantFile));
				}
				else {
					ObjectMapper m = getYamlMapper();
					JsonNode config = m.readTree(constantFile);
					toStore = addJsonNodeToProperties("", config, toStore);
				}
				getLog().info("Loaded " + toStore.size() + " constants");
			}
			else {
				getLog().info("No constant file found at: " + constantFile);
			}
			File targetFilterFile = new File(getBuildDir(), "constants.properties");
			toStore.store(new FileOutputStream(targetFilterFile), null);
		}
		catch (Exception e) {
			throw new MojoExecutionException("Unable to setup resource filter", e);
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
					copyAndFilterConfiguration(unpackDir, getPackageDir());
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
				executionEnvironment(mavenProject, mavenSession, pluginManager)
		);
	}

	/**
	 * This copies the configuration in the given directory into the target configuration
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
				executionEnvironment(mavenProject, mavenSession, pluginManager)
		);
	}

	protected void createArchive() throws MojoExecutionException {
		getLog().info("Creating archive");

		// Write descriptor xml
		String assemblyFileName = "packager-config-assembly.xml";
		File assemblyFile = new File(getBuildDir(), assemblyFileName);
		copyResourceToFile(assemblyFileName, assemblyFile);

		executeMojo(
				plugin("org.apache.maven.plugins", "maven-assembly-plugin", "3.1.0"),
				goal("single"),
				configuration(
						element("appendAssemblyId", "false"),
						element("descriptors",
								element("descriptor", assemblyFile.getAbsolutePath())
						)
				),
				executionEnvironment(mavenProject, mavenSession, pluginManager)
		);
	}

	private ObjectMapper getYamlMapper() {
		return new ObjectMapper(new YAMLFactory());
	}

	private Properties addJsonNodeToProperties(String propertyName, JsonNode node, Properties p) {
		if (node.isObject()) {
			getLog().debug("Adding object at:" + propertyName);
			ObjectNode objectNode = (ObjectNode) node;
			for (Iterator<Map.Entry<String, JsonNode>> i = objectNode.fields(); i.hasNext();) {
				Map.Entry<String, JsonNode> entry = i.next();
				propertyName += (propertyName != null && !propertyName.equals("") ? "." : "") + entry.getKey();
				addJsonNodeToProperties(propertyName, entry.getValue(), p);
			}
		}
		else if (node.isArray()) {
			ArrayNode arrayNode = (ArrayNode) node;
			for (int i = 0; i < arrayNode.size(); i++) {
				addJsonNodeToProperties(propertyName + "[" + i + "]", arrayNode.get(i), p);
			}
		}
		else if (node.isValueNode()) {
			ValueNode valueNode = (ValueNode) node;
			getLog().debug("Adding value at: " + propertyName + " = " + valueNode.textValue());
			p.put(propertyName, valueNode.textValue());
		}
		return p;
	}

	private void copyResourceToFile(String resource, File file) throws MojoExecutionException {
		InputStream is = null;
		OutputStream os = null;
		try {
			is = getClass().getClassLoader().getResourceAsStream(resource);
			os = new FileOutputStream(file);
			IOUtils.copy(is, os);
		}
		catch (Exception e) {
			throw new MojoExecutionException("Error copying " + resource + " to " + file, e);
		}
		finally {
			IOUtils.closeQuietly(is);
			IOUtils.closeQuietly(os);
		}
	}

	private File getBuildDir() {
		File dir = new File(mavenProject.getBuild().getDirectory());
		if (!dir.exists()) {
			dir.mkdirs();
		}
		return dir;
	}

	private File getPackageDir() {
		// Note, if this changes, need to change packager-config-assembly.xml
		File dir = new File(getBuildDir(), "generated-config-package");
		if (!dir.exists()) {
			dir.mkdirs();
		}
		return dir;
	}
}
