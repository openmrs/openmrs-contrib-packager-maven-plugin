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

import static org.twdata.maven.mojoexecutor.MojoExecutor.executionEnvironment;

import java.io.File;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.BuildPluginManager;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.project.MavenProject;
import org.twdata.maven.mojoexecutor.MojoExecutor;

/**
 * Packages a standard set of OpenMRS configurations
 */
public abstract class AbstractPackagerConfigMojo extends AbstractMojo {

	@Component
	private MavenProject mavenProject;

	@Component
	private MavenSession mavenSession;

	@Component
	private BuildPluginManager pluginManager;

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
	 * @return a standard Yaml mapper that can be used by all Yaml processing Mojos
	 */
	protected ObjectMapper getYamlMapper() {
		return new ObjectMapper(new YAMLFactory());
	}
}
