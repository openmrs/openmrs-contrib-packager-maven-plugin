package org.openmrs.maven.plugins.packager.config;

import java.io.File;

import junit.framework.TestCase;

public class PackageConfigurationsTest extends TestCase {

	/**
	 * @throws Exception
	 */
	public void testConfigurationsPackagedIntoZip() throws Exception {
		ConfigProject configProject = new ConfigProject("config-test-parent");
		configProject.executeGoal("clean", "-N", "-X");
		configProject.executeGoal("package", "-N", "-X");

		File zipFile = new File(configProject.getTargetDir(), "openmrs-packager-maven-plugin-config-test-parent-1.0.0-SNAPSHOT.zip");
		configProject.testFileExists(zipFile);
	}
}
