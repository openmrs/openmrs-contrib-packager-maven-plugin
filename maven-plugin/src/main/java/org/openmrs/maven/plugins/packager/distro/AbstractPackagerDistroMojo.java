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

import java.io.File;

import org.apache.maven.plugins.annotations.Parameter;
import org.openmrs.maven.plugins.packager.AbstractPackagerMojo;

/**
 * Superclass for Mojo within the distro package
 */
public abstract class AbstractPackagerDistroMojo extends AbstractPackagerMojo {

	@Parameter(property = "artifactDir", defaultValue = "${project.build.directory}/openmrs-packager-distro/package-distribution/artifacts")
	File artifactDir;

	/**
	 * Convenience method to get the project build directory
	 */
	protected File getPluginBuildDir() {
		File baseBuildDir = getBuildDir();
		File pluginBuildDir = new File(baseBuildDir, "openmrs-packager-distro");
		if (!pluginBuildDir.exists()) {
			pluginBuildDir.mkdirs();
		}
		return pluginBuildDir;
	}
}
