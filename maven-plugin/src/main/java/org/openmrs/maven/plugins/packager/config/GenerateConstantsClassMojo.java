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

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Paths;
import java.util.Map;
import java.util.Properties;
import java.util.TreeMap;

import static org.twdata.maven.mojoexecutor.MojoExecutor.configuration;
import static org.twdata.maven.mojoexecutor.MojoExecutor.element;
import static org.twdata.maven.mojoexecutor.MojoExecutor.executeMojo;
import static org.twdata.maven.mojoexecutor.MojoExecutor.goal;
import static org.twdata.maven.mojoexecutor.MojoExecutor.plugin;

/**
 * This mojo has been adapted from https://github.com/I-TECH/openmrs-contrib-maven-plugin-distrotools
 * This goal is to generate a constants file from a constants.properties resource defined in given dependency zip
 */
@Mojo(name = "generate-constants-class", defaultPhase = LifecyclePhase.GENERATE_SOURCES)
public class GenerateConstantsClassMojo extends AbstractPackagerConfigMojo {

	// The groupId for the dependency that contains the constants.properties
	@Parameter(property = "groupId", required = true)
	private String groupId;

	// The artifactId for the dependency that contains the constants.properties
	@Parameter(property = "artifactId", required = true)
	private String artifactId;

	// The version for the dependency that contains the constants.properties
	@Parameter(property = "version", required = true)
	private String version;

	// The package name for the generated class
	@Parameter(property = "packageName", required = true)
	private String packageName;

	// The class name for the generated constants file
	@Parameter(property = "className", defaultValue = "Constants")
	private String className;

	/**
	 * Executes the generate goal
	 * @throws org.apache.maven.plugin.MojoExecutionException if an error occurs
	 */
	public void execute() throws MojoExecutionException {

		String template = "";
		try (InputStream is = getClass().getClassLoader().getResourceAsStream("Constants.java.template")) {
			template = IOUtils.toString(is);
		}
		catch (IOException e) {
			throw new MojoExecutionException("Unable to load java template", e);
		}

		// Load in the dependent project
		ConfigDependency d = new ConfigDependency(groupId, artifactId, version);
		unpackDependency(d, getPluginSourcesDir());
		Properties constants = loadPropertiesFromFile(new File(getPluginSourcesDir(), "constants.properties"));

		Map<String, String> sortedConstants = new TreeMap<>();
		for (Object key : constants.keySet()) {
			String varName = key.toString().replace(".", "_").toUpperCase();
			String varVal = constants.getProperty(key.toString());
			sortedConstants.put(varName, varVal);
		}

		StringBuilder sb = new StringBuilder();
		for (String key : sortedConstants.keySet()) {
			String val = sortedConstants.get(key);
			sb.append("\n\tpublic static final String ").append(key).append(" = \"").append(val).append("\";");
		}

		template = template.replace("{CLASS_NAME}", className);
		template = template.replace("{PACKAGE}", packageName);
		template = template.replace("{REFERENCES}", sb.toString());

		File outputDir = new File(getGeneratedSourcesDir(), packageName.replace(".", File.separator));
		if (!outputDir.exists()) {
			outputDir.mkdirs();
		}

		File outputFile = new File(outputDir, className + ".java");
		try {
			FileUtils.writeStringToFile(outputFile, template, "UTF-8");
			getLog().info("Generated " + outputFile.getPath());
		}
		catch (Exception e) {
			throw new MojoExecutionException("An error occurred writing to file", e);
		}

		executeMojo(
				plugin("org.codehaus.mojo", "build-helper-maven-plugin", "1.8"),
				goal("add-source"),
				configuration(
						element("sources",
								element("source", getGeneratedSourcesDir().getAbsolutePath())
						)
				),
				getMavenExecutionEnvironment()
		);
	}

	/**
	 * @return the directory which will contain the resulting compiled configuration
	 */
	public File getPluginSourcesDir() {
		return Paths.get(getPluginBuildDir().getPath(), "generate-constants-class").toFile();
	}

	/**
	 * @return the directory which will contain the resulting compiled configuration
	 */
	public File getGeneratedSourcesDir() {
		return new File(getBuildDir(), "generated-sources");
	}
}