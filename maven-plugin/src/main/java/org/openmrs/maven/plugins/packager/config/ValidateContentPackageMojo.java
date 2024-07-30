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

import com.vdurmont.semver4j.Semver;
import com.vdurmont.semver4j.SemverException;

/**
 * The purpose of this Mojo is to validate content properties file - validates the properties are
 * only in ranges. values like latest and next are not allowed.
 */
@Mojo(name = "validate-content-package")
public class ValidateContentPackageMojo extends AbstractMojo {
	
	private static String RANGE_REGEX = ".*(\\^|~|>|>=|<|<=|\\|\\|| - ).*";
	
	// conf properties file
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
			
			for (String key : properties.stringPropertyNames()) {
				if ("name".equalsIgnoreCase(key)) {
					continue;
				}
				
				String value = properties.getProperty(key);
				
				if ("version".equalsIgnoreCase(key)) {
					new Semver(value.trim(), Semver.SemverType.NPM);
					continue;
				}
				if (!isValid(value)) {
					throw new MojoExecutionException("Invalid version format for key: " + key + ", value: " + value
					        + ", please provide version as a valid semver range");
				}
			}
		}
		catch (Exception e) {
			throw new MojoExecutionException(sourceFile + "-" + e.getMessage());
		}
	}
	
	protected boolean isValid(String value) {
		if (isRange(value)) {
			return isValidVersionRange(value);
		}
		return false;
	}
	
	private boolean isRange(String input) {
		return input.matches(RANGE_REGEX);
	}
	
	private boolean isValidVersionRange(String range) {
		try {
			if (range.contains(" - ")) {
				String[] parts = range.split(" - ");
				new Semver(parts[0].trim(), Semver.SemverType.NPM);
				new Semver(parts[1].trim(), Semver.SemverType.NPM);
				return true;
			} else {
				// Additional checks for other range types (e.g., ^, ~, >, <)
				// we might need to implement further checks or use a library that supports these
				new Semver("1.0.0", Semver.SemverType.NPM).satisfies(range); // Dummy check
				return true;
			}
		}
		catch (SemverException e) {
			return false;
		}
	}
	
}
