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

import org.apache.commons.io.FileUtils;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.Properties;

import static org.twdata.maven.mojoexecutor.MojoExecutor.configuration;
import static org.twdata.maven.mojoexecutor.MojoExecutor.element;
import static org.twdata.maven.mojoexecutor.MojoExecutor.executeMojo;
import static org.twdata.maven.mojoexecutor.MojoExecutor.goal;
import static org.twdata.maven.mojoexecutor.MojoExecutor.plugin;

/**
 * The purpose of this Mojo is to support the migration away from config packages to content packages
 * The intent is to bundle up the same configurations as content packages
 */
@Mojo(name = "create-content-package", defaultPhase = LifecyclePhase.COMPILE)
public class CreateContentPackageMojo extends AbstractPackagerConfigMojo {

	@Parameter(property = "name", defaultValue = "${project.name}")
	private String name;

	@Parameter(property = "version", defaultValue = "${project.version}")
	private String version;

	@Parameter(property = "sourceConfigurationDir", defaultValue = "${project.basedir}/configuration")
	private File sourceConfigurationDir;

	@Parameter(property = "sourceFrontendSubdir", defaultValue = "frontend")
	private String sourceFrontendSubdir;

	// Dependency configurations that this project wishes to import from other projects
	@Parameter(property = "dependencyFile", defaultValue = "dependencies.yml")
	private File dependenciesFile;

	@Parameter(property = "targetDir", defaultValue = "${project.build.directory}/package")
	private File targetDir;

	/**
	 * @throws MojoExecutionException if an error occurs
	 */
	public void execute() throws MojoExecutionException {
		List<String> contentProperties = new ArrayList<>();
		contentProperties.add("# Content Package");
		contentProperties.add("name=" + name);
		contentProperties.add("version=" + version);

		Properties constants = loadPropertiesFromFile(getCompiledConstantsFile());
		if (constants != null && !constants.isEmpty()) {
			contentProperties.add("");
			contentProperties.add("# Constants");
			Enumeration<Object> e = constants.keys();
			while (e.hasMoreElements()) {
				Object key = e.nextElement();
				contentProperties.add("var." + key + "=" + constants.get(key));
			}
		}

		File contentPropertiesFile = new File(targetDir, "content.properties");
		try {
			FileUtils.writeLines(contentPropertiesFile, "UTF-8", contentProperties);
		}
		catch (IOException e) {
			throw new MojoExecutionException("Unable to write content.properties", e);
		}

		File targetConfigurationDir = new File(targetDir, "configuration");
		File backendTargetDir = new File(targetConfigurationDir, "backend_configuration");

		executeMojo(
				plugin("org.apache.maven.plugins", "maven-resources-plugin", "3.1.0"),
				goal("copy-resources"),
				configuration(
						element("outputDirectory", backendTargetDir.getAbsolutePath()),
						element("encoding", "UTF-8"),
						element("overwrite", "true"),
						element("resources",
								element("resource",
										element("directory", sourceConfigurationDir.getAbsolutePath()),
										element("filtering", "false"),
										element("includes",
												element("include", "**/*")
										),
										element("excludes",
											element("exclude", sourceFrontendSubdir + "/**/*")
										)
								)
						)
				),
				getMavenExecutionEnvironment()
		);

		File frontendSourceDir = new File(sourceConfigurationDir, sourceFrontendSubdir);
		File frontendTargetDir = new File(targetConfigurationDir, "frontend_configuration");

		executeMojo(
				plugin("org.apache.maven.plugins", "maven-resources-plugin", "3.1.0"),
				goal("copy-resources"),
				configuration(
						element("outputDirectory", frontendTargetDir.getAbsolutePath()),
						element("encoding", "UTF-8"),
						element("overwrite", "true"),
						element("resources",
								element("resource",
										element("directory", frontendSourceDir.getAbsolutePath()),
										element("filtering", "false"),
										element("includes",
												element("include", "**/*")
										)
								)
						)
				),
				getMavenExecutionEnvironment()
		);

	}
}
