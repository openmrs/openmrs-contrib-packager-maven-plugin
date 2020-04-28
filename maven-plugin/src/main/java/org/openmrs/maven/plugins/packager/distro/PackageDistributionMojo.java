/**
 * This Source Code Form is subject to the terms of the Mozilla Public License,
 * v. 2.0. If a copy of the MPL was not distributed with this file, You can
 * obtain one at http://mozilla.org/MPL/2.0/. OpenMRS is also distributed under
 * the terms of the Healthcare Disclaimer located at http://openmrs.org/license.
 *
 * Copyright (C) OpenMRS Inc. OpenMRS is a registered trademark and the OpenMRS
 * graphic logo is a trademark of OpenMRS Inc.
 */
package org.openmrs.maven.plugins.packager.distro;

import static org.twdata.maven.mojoexecutor.MojoExecutor.configuration;
import static org.twdata.maven.mojoexecutor.MojoExecutor.element;
import static org.twdata.maven.mojoexecutor.MojoExecutor.executeMojo;
import static org.twdata.maven.mojoexecutor.MojoExecutor.goal;
import static org.twdata.maven.mojoexecutor.MojoExecutor.plugin;

import java.io.File;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.openmrs.maven.plugins.packager.config.AbstractPackagerConfigMojo;

/**
 * The purpose of this Mojo is to package up an OpenMRS distribution into a Zip artifact
 */
@Mojo(name = "package-distribution", defaultPhase = LifecyclePhase.PACKAGE)
public class PackageDistributionMojo extends AbstractPackagerConfigMojo {

	/**
	 * @throws MojoExecutionException if an error occurs
	 */
	public void execute() throws MojoExecutionException {
		createArchive();
	}

	/**
	 * Executes the Maven assembly plugin in order to create a packaged zip artifact
	 */
	protected void createArchive() throws MojoExecutionException {
		getLog().info("Creating archive");

		// Write descriptor xml
		String assemblyFileName = "packager-distro-assembly.xml";
		File assemblyFile = new File(getPluginBuildDir(), assemblyFileName);
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
				getMavenExecutionEnvironment()
		);
	}
}
