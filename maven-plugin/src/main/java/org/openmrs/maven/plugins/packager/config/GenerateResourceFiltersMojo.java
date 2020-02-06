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
import java.util.Iterator;
import java.util.Map;
import java.util.Properties;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.ValueNode;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;

/**
 * The purpose of this Mojo is to allow either a standard properties file, or
 * a YAML file to serve as the source of constants when processing other resources.
 * This will take in an input file (default os constants.yml) and will product an output file that can be used as a filter
 */
@Mojo(name = "generate-resource-filters", defaultPhase = LifecyclePhase.GENERATE_RESOURCES)
public class GenerateResourceFiltersMojo extends AbstractPackagerConfigMojo {

	@Parameter(property = "sourceFile", defaultValue = "${project.basedir}/constants.yml")
	private File sourceFile;

	/**
	 * @throws MojoExecutionException if an error occurs
	 */
	public void execute() throws MojoExecutionException {
		setupResourceFilters();
	}

	/**
	 * Copies source properties or yml to target properties
	 */
	protected void setupResourceFilters() throws MojoExecutionException {
		Properties toStore = new ConstantProperties();
		try {
			if (sourceFile != null && sourceFile.exists()) {
				getLog().info("Source file found at: " + sourceFile);

				if (sourceFile.getAbsolutePath().endsWith(".properties")) {
					toStore.load(new FileInputStream(sourceFile));
				}
				else {
					ObjectMapper m = getYamlMapper();
					JsonNode config = m.readTree(sourceFile);
					toStore = addJsonNodeToProperties("", config, toStore);
				}
				getLog().info("Loaded " + toStore.size() + " constants");
			}
			else {
				getLog().info("No constant file found at: " + sourceFile);
			}
			ensureCompiledConfigurationDir();
			savePropertiesToFile(toStore, getCompiledConstantsFile());
		}
		catch (Exception e) {
			throw new MojoExecutionException("Unable to setup resource filter", e);
		}
	}

	/**
	 * Converts from a yml file to a properties file, using:
	 *   - dot notation (eg. object1.nestedObject2.property)
	 *   - array notation (eg. object1.nestedArray2[0].property)
	 */
	private Properties addJsonNodeToProperties(String propertyName, JsonNode node, Properties p) {
		getLog().debug("Adding json node to properties: " + propertyName);
		if (node.isObject()) {
			getLog().debug("Node is an object");
			ObjectNode objectNode = (ObjectNode) node;
			for (Iterator<Map.Entry<String, JsonNode>> i = objectNode.fields(); i.hasNext();) {
				Map.Entry<String, JsonNode> entry = i.next();
				String newPropertyName = entry.getKey();
				if (propertyName != null && !propertyName.equals("")) {
					newPropertyName = propertyName + "." + newPropertyName;
				}
				addJsonNodeToProperties(newPropertyName, entry.getValue(), p);
			}
		}
		else if (node.isArray()) {
			getLog().debug("Node is an array");
			ArrayNode arrayNode = (ArrayNode) node;
			for (int i = 0; i < arrayNode.size(); i++) {
				addJsonNodeToProperties(propertyName + "[" + i + "]", arrayNode.get(i), p);
			}
		}
		else if (node.isValueNode()) {
			getLog().debug("Node is a value.");
			ValueNode valueNode = (ValueNode) node;
			String value = valueNode.textValue();
			if (value != null) {
				getLog().debug("Adding value at: " + propertyName + " = " + value);
				p.put(propertyName, value);
			}
			else {
				getLog().warn("Value is null");
			}
		}
		return p;
	}
}
