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

import java.io.File;
import java.io.Serializable;

/**
 * Describes an artifact
 */
public class ConfigDependency implements Serializable {

	private String groupId;
	private String artifactId;
	private String version;

	public ConfigDependency() {}

	@Override
	public String toString() {
		return toString(":");
	}

	public String toString(String separator) {
		return groupId + separator + artifactId + separator + version;
	}

	public File getPathInRepository(String baseDir) {
		StringBuilder sb = new StringBuilder(baseDir);
		String ps = System.getProperty("file.separator");
		sb.append(ps).append(getGroupId().replace(".", ps));
		sb.append(ps).append(getArtifactId()).append(ps).append(getVersion());
		return new File(sb.toString());
	}

	public String getGroupId() {
		return groupId;
	}

	public void setGroupId(String groupId) {
		this.groupId = groupId;
	}

	public String getArtifactId() {
		return artifactId;
	}

	public void setArtifactId(String artifactId) {
		this.artifactId = artifactId;
	}

	public String getVersion() {
		return version;
	}

	public void setVersion(String version) {
		this.version = version;
	}
}
