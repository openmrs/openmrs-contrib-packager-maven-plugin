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

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
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
		Properties toStore = new Properties();
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
			File targetFile = new File(getBuildDir(), "constants.properties");
			toStore.store(new FileOutputStream(targetFile), null);
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
}
