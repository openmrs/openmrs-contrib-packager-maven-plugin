package org.openmrs.maven.plugins.packager.config;

import java.io.File;

import org.junit.Assert;
import org.junit.Test;

public class PackageConfigurationsTest {

	@Test
	public void testConfigurationsPackagedIntoZip() throws Exception {
		ConfigProject configProject = new ConfigProject("config-test-parent");
		configProject.executeGoal("clean", "-N", "-X");
		configProject.executeGoal("package", "-N", "-X");

		File zipFile = new File(configProject.getTargetDir(), "openmrs-packager-maven-plugin-config-test-parent-1.0.0-SNAPSHOT.zip");
		Assert.assertTrue(zipFile.exists());
	}
}
