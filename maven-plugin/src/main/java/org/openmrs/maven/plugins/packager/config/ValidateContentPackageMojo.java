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

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;

import com.github.zafarkhaja.semver.expr.ExpressionParser;

/**
 * The purpose of this Mojo is to validate content properties file - validates the properties are
 * only in ranges. Values like latest and next are not allowed.
 */
@Mojo(name = "validate-content-package")
public class ValidateContentPackageMojo extends AbstractMojo {
	
	private static String RANGE_REGEX = ".*(\\^|~|>|>=|<|<=|\\|\\|| - ).*";
	
	/**
	 * The full path to the content.properties file, including the filename. The content.properties
	 * file is similar to distro.properties configuration used in the validation process. For
	 * example: "{project.basedir}/content.properties".
	 */
	@Parameter(property = "sourceFile")
	protected String sourceFile;
	
	/**
	 * Executes the property validation.
	 *
	 * @throws MojoExecutionException if an error occurs during validation
	 */
	public void execute() throws MojoExecutionException {
		validateProperties();
	}
	
	/**
	 * Validates the properties in the given file.
	 *
	 * @throws IOException if an error occurs while reading the file
	 */
	private void validateProperties() throws MojoExecutionException {
		try (InputStream inputStream = new FileInputStream(sourceFile)) {
			Properties properties = new Properties();
			properties.load(inputStream);
			
			if (!properties.containsKey("name") || !properties.containsKey("version")) {
				throw new MojoExecutionException("The properties file must contain both 'name' and 'version' keys.");
			}
			
			for (String key : properties.stringPropertyNames()) {
				String value = properties.getProperty(key);
				
				if (key.startsWith("omod") || key.startsWith("owa") || key.startsWith("spa.frontend")) {
					if (!isValid(value)) {
						throw new MojoExecutionException("Invalid SemVer format for key: " + key + ", value: " + value
						        + ", Valid semver format expected");
					}
				}
			}
		}
		catch (Exception e) {
			throw new MojoExecutionException(sourceFile + " - " + e.getMessage());
		}
	}
	
	/**
	 * Validates whether a given value is a valid SemVer expression or range.
	 *
	 * @param value the value to validate
	 * @return true if the value is a valid SemVer expression or range, false otherwise
	 */
	protected boolean isValid(String value) {
		try {
			ExpressionParser.newInstance().parse(value);
			return true;
		}
		catch (Exception e) {
			return false;
		}
	}
}
