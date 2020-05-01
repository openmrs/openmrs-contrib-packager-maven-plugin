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

import static org.twdata.maven.mojoexecutor.MojoExecutor.executionEnvironment;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.BuildPluginManager;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;
import org.twdata.maven.mojoexecutor.MojoExecutor;

/**
 * Packages a standard set of OpenMRS configurations
 */
public abstract class AbstractPackagerMojo extends AbstractMojo {

	@Parameter( defaultValue = "${project}", readonly = true )
	MavenProject mavenProject;

	@Parameter( defaultValue = "${session}", readonly = true )
	MavenSession mavenSession;

	@Component
	BuildPluginManager pluginManager;

	/**
	 * Convenience method to get the execution environment for invoking other Maven plugins
	 */
	protected MojoExecutor.ExecutionEnvironment getMavenExecutionEnvironment() {
		return executionEnvironment(mavenProject, mavenSession, pluginManager);
	}

	/**
	 * Convenience method to get the source directory for this project
	 */
	protected File getBaseDir() {
		return mavenProject.getBasedir();
	}

	/**
	 * Convenience method to get the build directory used by all plugins.  This is typically "/target"
	 */
	protected File getBuildDir() {
		return new File(mavenProject.getBuild().getDirectory());
	}

	/**
	 * Convenience method to get access to the Local Repository
	 */
	protected ArtifactRepository getLocalRepository() {
		return mavenSession.getLocalRepository();
	}

	/**
	 * @return a standard Yaml mapper that can be used by all Yaml processing Mojos
	 */
	protected ObjectMapper getYamlMapper() {
		return new ObjectMapper(new YAMLFactory());
	}

	/**
	 * Convenience method to read a File resource to String
	 */
	protected String getResourceAsString(String resource) throws MojoExecutionException {
		InputStream is = null;
		try {
			is = getClass().getClassLoader().getResourceAsStream(resource);
			return IOUtils.toString(is, "UTF-8");
		}
		catch (Exception e) {
			throw new MojoExecutionException("Error reading " + resource, e);
		}
		finally {
			IOUtils.closeQuietly(is);
		}
	}

	protected void writeStringToFile(File file, String s) throws MojoExecutionException {
		try {
			FileUtils.writeStringToFile(file, s, "UTF-8");
		}
		catch (Exception e) {
			throw new MojoExecutionException("An error occurred writing to file: " + file);
		}
	}

	/**
	 * Convenience method to copy from a classpath resource to a File
	 */
	protected void copyResourceToFile(String resource, File file) throws MojoExecutionException {
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
}
