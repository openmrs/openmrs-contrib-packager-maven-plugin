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

import java.io.File;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

import org.apache.commons.io.FileUtils;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.model.Dependency;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;
import org.openmrs.maven.plugins.packager.Plugins;

/**
 * The purpose of this Mojo is to create an openmrs-distro.properties file
 */
@Mojo(name = "build-distro-properties", defaultPhase = LifecyclePhase.GENERATE_RESOURCES)
public class BuildDistroPropertiesMojo extends AbstractPackagerDistroMojo {

	@Parameter(property = "owas")
	List<Dependency> owas;

	/**
	 * @throws MojoExecutionException if an error occurs
	 */
	public void execute() throws MojoExecutionException {
		MavenProject mp = getMavenExecutionEnvironment().getMavenProject();

		Map<String, String> props = new LinkedHashMap<>();

		props.put("name", mp.getName());
		props.put("version", mp.getVersion());
		props.put("war.openmrs", getArtifact("openmrs-webapp", "war").getVersion());

		// For every dependency declared that is of type jar or omod, add it if it is determined to be a module artifact
		Map<String, String> sortedModules = new TreeMap<>();
		for (Artifact a : mp.getDependencyArtifacts()) {
			if (isModule(a)) {
				String moduleKey = "omod." + a.getArtifactId().replace("-omod", "");
				sortedModules.put(moduleKey, a.getVersion());
				if (a.getType().equalsIgnoreCase("omod")) {
					sortedModules.put(moduleKey + ".type", a.getType());
				}
			}
		}
		props.putAll(sortedModules);

		// Since owas are not managed in maven, they are simply added in as dependencies to populate the distro properties
		if (owas != null) {
			for (Dependency d : owas) {
				props.put("owa." + d.getArtifactId(), d.getVersion());
			}
		}

		// Add remaining standard properties
		props.put("db.h2.supported", "false");

		File outputFile = new File(getPluginBuildDir(), "openmrs-distro.properties");
		List<String> outputLines = new ArrayList<>();
		for (String key : props.keySet()) {
			String val = props.get(key);
			outputLines.add(key + "=" + val);
		}
		try {
			FileUtils.writeLines(outputFile, "UTF-8", outputLines);
		}
		catch (Exception e) {
			throw new MojoExecutionException("Error writing to output file " + outputFile, e);
		}

		executeMojo(
				Plugins.MAVEN_RESOURCES_PLUGIN,
				goal("copy-resources"),
				configuration(
						element("outputDirectory", getTargetDir().getAbsolutePath()),
						element("includeEmptyDirs", "true"),
						element("resources",
								element("resource",
										element("directory", getPluginBuildDir().getAbsolutePath()),
										element("filtering", "false")
								)
						)
				),
				getMavenExecutionEnvironment()
		);
	}

	/**
	 * @return true if the passed Artifact is a module, false otherwise
	 * First, a check is done that the artifact is either of type jar or omod
	 * If so, this attempts to unpack the jar and confirm that it can do so and that a config.xml file exists
	 */
	public boolean isModule(Artifact a) {
		if (a.getType().equalsIgnoreCase("jar") || a.getType().equalsIgnoreCase("omod")) {
			try {
				JarFile jarFile = new JarFile(a.getFile());
				Enumeration<? extends JarEntry> jarEntries = jarFile.entries();
				while (jarEntries.hasMoreElements()) {
					JarEntry entry = jarEntries.nextElement();
					;
					getLog().info("Checking entry: " + entry.getName());
					if (entry.getName().equals("config.xml")) {
						return true;
					}
				}
			}
			catch (Exception e) {
				getLog().warn("Unable to inspect artifact " + a + " to determine if it is a module", e);
			}
		}
		return false;
	}

	public Artifact getArtifact(String artifactId, String type) throws MojoExecutionException {
		List<Artifact> l = new ArrayList<>();
		for (Artifact a : getMavenExecutionEnvironment().getMavenProject().getDependencyArtifacts()) {
			if (artifactId == null || artifactId.equalsIgnoreCase(a.getArtifactId())) {
				if (type == null || type.equalsIgnoreCase(a.getType())) {
					l.add(a);
				}
			}
		}
		if (l.isEmpty()) {
			throw new MojoExecutionException("Unable to find dependency " + artifactId + " of type " + type);
		}
		else if (l.size() > 1) {
			throw new MojoExecutionException("More than one dependency with artifactId " + artifactId + ", type: " + type);
		}
		return l.get(0);
	}

	/**
	 * @return the directory into which to write the resources
	 */
	@Override
	public File getPluginBuildDir() {
		File sourcesDir = new File(super.getPluginBuildDir(), "build-distro-properties");
		if (!sourcesDir.exists()) {
			sourcesDir.mkdirs();
		}
		return sourcesDir;
	}

	/**
	 * @return the compiled classes dir, creating if necessary
	 */
	public File getTargetDir() {
		File classesDir = new File(getBuildDir(), "classes");
		if (!classesDir.exists()) {
			classesDir.mkdirs();
		}
		return classesDir;
	}
}
