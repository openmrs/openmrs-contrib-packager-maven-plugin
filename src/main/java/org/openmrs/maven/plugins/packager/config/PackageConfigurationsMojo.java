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
import static org.twdata.maven.mojoexecutor.MojoExecutor.goal;
import static org.twdata.maven.mojoexecutor.MojoExecutor.plugin;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;

import org.apache.commons.io.IOUtils;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;

/**
 * The purpose of this Mojo is to package up the compiled configurations into a Zip artifact
 */
@Mojo(name = "package-configurations", defaultPhase = LifecyclePhase.PACKAGE)
public class PackageConfigurationsMojo extends AbstractPackagerConfigMojo {

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
				getMavenExecutionEnvironment()
		);
	}

	/**
	 * Copies a classpath resource to the Filesystem
	 */
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

}
