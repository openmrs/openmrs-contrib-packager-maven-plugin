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
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.apache.maven.plugin.Mojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugin.logging.Log;

/**
 * This enables watching for file creation, deletion, and modifications to a set of directories
 * It allows for excluding one or more files from each of these watched directories
 * It allows for setting a Mojo to execute when changes are detected.
 * It allows for setting a time to wait in MS after the last change is detected before executing the Mojo, to allow
 * rapid editing of files to occur, and an update to take place only when that editing is paused.
 */
public class ConfigurationWatcher {

	private Log log;
	private WatchService watchService;
	private Map<WatchKey, Path> registeredKeys = new HashMap<>();
	private Mojo mojoToExecute;
	private long timeToWait;
	private long lastMojoExecutionTime = System.currentTimeMillis();
	private long lastModificationTime = -1;

	public ConfigurationWatcher(Log log, Mojo mojoToExecute, long timeToWait) {
		try {
			this.log = log;
			this.mojoToExecute = mojoToExecute;
			this.timeToWait = timeToWait;
			watchService = FileSystems.getDefault().newWatchService();
		}
		catch (Exception e) {
			throw new IllegalStateException("Unable to set up a watcher", e);
		}
	}

	/**
	 * Register the passed sourceDirectory to watch recursively.  All subdirectories will be registered to watch as well.
	 * Any filesOrDirectoriesToIgnore that are passed in enable specific files within the sourceDirectory to be excluded
	 * from watching.  Typical usage would be to watch the entire project directory, but ignore the target/build directory.
	 *
	 * This does not actually start the watcher, it only configures it with specific files or directories, and it can be
	 * called as many times as needed with specific directories to add with exclusions.
	 */
	public void registerDirectoryToWatch(File sourceDirectory, final File... filesOrDirectoriesToIgnore) {
		log.info("Watching files in:  " + sourceDirectory);
		log.info(("Ignoring files that match: " + Arrays.asList(filesOrDirectoriesToIgnore)));
		try {
			Files.walkFileTree(sourceDirectory.toPath(), new SimpleFileVisitor<Path>() {
				public FileVisitResult preVisitDirectory(Path path, BasicFileAttributes attributes) throws IOException {
					if (isConfiguredToIgnore(path, filesOrDirectoriesToIgnore)) {
						log.debug("Ignored path: " + path);
					}
					else {
						WatchKey watchKey = path.register(watchService, ENTRY_CREATE, ENTRY_MODIFY, ENTRY_DELETE);
						registeredKeys.put(watchKey, path);
						log.debug("Registered a watcher with path: " + path);
					}
					return FileVisitResult.CONTINUE;
				}
			});
		}
		catch (IOException e) {
			throw new IllegalStateException("Error setting directories to watch", e);
		}
	}

	/**
	 * This is the method that actually starts the watcher and listens for file or directory creation, modification,
	 * or deletion events for those files that were registered to watch.
	 */
	public void watch() throws MojoExecutionException, MojoFailureException {
		// Setup an infinite loop to watch for new change notifications
		while (true) {
			WatchKey key = null;
			try {
				key = watchService.poll(timeToWait, TimeUnit.MILLISECONDS);

				// If key is null then no changes have been detected in the specified time to wait.
				// If modifications have been made since the last time the Mojo was executed, then execute it here
				if (key == null) {
					if (lastModificationTime > lastMojoExecutionTime) {
						log.info("Changes detected, updating configuration...");
						mojoToExecute.execute();
						lastMojoExecutionTime = System.currentTimeMillis();
					}
				}
				// If key is not null, then changes have been detected, so active work is ongoing, do not copy over
				else {
					Path path = registeredKeys.get(key);
					for (WatchEvent<?> event : key.pollEvents()) {
						if (event.context() instanceof Path) {
							Path eventContextPath = (Path) event.context();
							Path resolvedEventContextPath = path.resolve(eventContextPath);
							log.debug(event.kind().name() + ": " + path);

							// If a new directory is created, ensure we watch this to pick up file changes within
							if (Files.isDirectory(resolvedEventContextPath) && event.kind() == ENTRY_CREATE) {
								registerDirectoryToWatch(resolvedEventContextPath.toFile());
							}

							lastModificationTime = System.currentTimeMillis();
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
