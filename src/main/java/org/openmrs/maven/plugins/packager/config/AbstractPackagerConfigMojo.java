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
	 * Convenience method to get the project build directory
	 */
	protected File getBuildDir() {
		File baseBuildDir = new File(mavenProject.getBuild().getDirectory());
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
