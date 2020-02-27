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

import static java.nio.file.StandardWatchEventKinds.ENTRY_CREATE;
import static java.nio.file.StandardWatchEventKinds.ENTRY_DELETE;
import static java.nio.file.StandardWatchEventKinds.ENTRY_MODIFY;

import java.io.File;
import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.WatchEvent;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.io.FileUtils;
import org.apache.maven.it.Verifier;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;

/**
 * The purpose of this Mojo is to watch the project for changes and execute
 * the specified goals if any files are changed
 */
@Mojo(name = "watch", defaultPhase = LifecyclePhase.NONE)
public class WatchConfigurationsMojo extends AbstractPackagerConfigMojo {

	@Parameter(property = "dependencyFile", defaultValue = "dependencies.yml")
	private File dependenciesFile;

	/**
	 * @throws MojoExecutionException if an error occurs
	 */
	public void execute() throws MojoExecutionException {
		try {
			// Get configuration options off of runtime arguments
			// DelaySeconds is the amount of time to wait after the last changes is made before executing goal (default 5)
			final int delaySeconds = Integer.parseInt(System.getProperty("delaySeconds", "5"));
			final String goalToRun = System.getProperty("goal", "install");

			// Set up a watch service that will look for file changes
			WatchService watchService = FileSystems.getDefault().newWatchService();
			Map<WatchKey, Path> registeredKeys = new HashMap<>();

			// Set up directories to ignore
			// For now, hard code directories to ignore for watching at the build dir and the git dir within a project
			File[] ignoredPaths = { getBuildDir(), new File(getBaseDir(), ".git") };

			// Register the source directory (recursively)
			registerDirectoryToWatch(watchService, registeredKeys, getBaseDir(), ignoredPaths);

			// Also watch any dependencies that change in the local repository
			if (dependenciesFile != null && dependenciesFile.exists()) {
				ObjectMapper m = getYamlMapper();
				List<ConfigDependency> configDependencies = m.readValue(dependenciesFile, new TypeReference<List<ConfigDependency>>() {});
				if (configDependencies != null) {
					for (ConfigDependency d : configDependencies) {
						File dependencyDir = d.getPathInRepository(getLocalRepository().getBasedir());
						registerDirectoryToWatch(watchService, registeredKeys, dependencyDir, ignoredPaths);
					}
				}
			}

			// Set up a maven verifier which will be used to execute the build
			Verifier verifier = new Verifier(getBaseDir().getAbsolutePath());
			verifier.setAutoclean(false);
			String openmrsServerId = System.getProperty("serverId");
			if (openmrsServerId != null) {
				verifier.addCliOption("-DserverId="+openmrsServerId);
			}

			// Initializer log file for the watcher
			String watcherLogFileName = "watcher.log";
			File watcherLog = new File(getBuildDir(), watcherLogFileName);
			FileUtils.touch(watcherLog);
			verifier.setLogFileName("target/" + watcherLogFileName);

			long lastMojoExecutionTime = System.currentTimeMillis();
			long lastModificationTime = -1;

			// Setup an infinite loop to continuously check for new change notifications
			while (true) {
				WatchKey key = null;
				try {
					key = watchService.poll(delaySeconds, TimeUnit.SECONDS);

					// If key is null then no changes have been detected in the specified time to wait.
					// If modifications have been made since the last time the Mojo was executed, then execute it here
					if (key == null) {
						if (lastModificationTime > lastMojoExecutionTime) {
							getLog().info("Changes detected, running: " + goalToRun);
							long startMs = System.currentTimeMillis();
							try {
								verifier.executeGoal(goalToRun);
								long executionMs = System.currentTimeMillis() - startMs;
								getLog().info("Successfully completed " + goalToRun + " in " + executionMs + "ms");
							}
							catch (Exception e) {
								getLog().warn("Error executing " + goalToRun + ". See " + verifier.getLogFileName() + " for details.");
							}
							lastMojoExecutionTime = System.currentTimeMillis();
						}
					}
					// If key is not null, then changes have been detected, so active work is ongoing, do not copy over
					else {
						boolean isModificationMade = false;
						Path path = registeredKeys.get(key);
						for (WatchEvent<?> event : key.pollEvents()) {
							if (event.context() instanceof Path) {
								Path eventContextPath = (Path) event.context();
								Path resolvedEventContextPath = path.resolve(eventContextPath);

								getLog().debug(event.kind().name() + " " + event.context() + " in " + path);

								// If a new directory is created, ensure we watch this to pick up file changes within
								// Only mark this as a modification if there are non-ignored paths created
								// This mainly serves to ensure that the build doesn't run when the target directory is initially created
								if (Files.isDirectory(resolvedEventContextPath) && event.kind() == ENTRY_CREATE) {
									boolean isRegistered = registerDirectoryToWatch(watchService, registeredKeys, resolvedEventContextPath.toFile(), ignoredPaths);
									isModificationMade = isModificationMade || isRegistered;
								}
								else {
									isModificationMade = true;
								}

								if (isModificationMade) {
									lastModificationTime = System.currentTimeMillis();
								}
							}
						}
					}
				}
				catch (InterruptedException x) {
					return;
				}
				finally {
					if (key != null) {
						key.reset();
					}
				}
			}
		}
		catch (Exception e) {
			throw new MojoExecutionException("An error occurred while watching configurations", e);
		}
	}

	/**
	 * Register the passed sourceDirectory to watch recursively.  All subdirectories will be registered to watch as well.
	 * This does not actually start the watcher, it only configures it with specific files or directories, and it can be
	 * called as many times as needed with specific directories to add with exclusions.
	 * @return true if any new directories are registered to watch
	 */
	public boolean registerDirectoryToWatch(final WatchService watchService, final Map<WatchKey, Path> registeredKeys, File sourceDirectory, final File[] ignoredPaths) {
		int numInitialKeys = registeredKeys.size();
		try {
			Files.walkFileTree(sourceDirectory.toPath(), new SimpleFileVisitor<Path>() {
				public FileVisitResult preVisitDirectory(Path path, BasicFileAttributes attributes) throws IOException {
					if (isConfiguredToIgnore(path, ignoredPaths)) {
						getLog().debug("Ignored new path: " + path);
					}
					else {
						WatchKey watchKey = path.register(watchService, ENTRY_CREATE, ENTRY_MODIFY, ENTRY_DELETE);
						registeredKeys.put(watchKey, path);
						getLog().info("Registered watcher with new path: " + path);
					}
					return FileVisitResult.CONTINUE;
				}
			});
		}
		catch (IOException e) {
			throw new IllegalStateException("Error setting directories to watch", e);
		}
		int numNewKeys = registeredKeys.size() - numInitialKeys;
		return numNewKeys > 0;
	}

	/**
	 * Helper method to check if a path is included in the passed ignore list
	 * @return true if the passed path is included in the filesOrDirectoriesToIgnore, false otherwise
	 */
	private boolean isConfiguredToIgnore(Path pathToCheck, File... filesOrDirectoriesToIgnore) {
		for (File fileToIgnore : filesOrDirectoriesToIgnore) {
			if (pathToCheck.startsWith(fileToIgnore.toPath())) {
				return true;
			}
		}
		return false;
	}
}
