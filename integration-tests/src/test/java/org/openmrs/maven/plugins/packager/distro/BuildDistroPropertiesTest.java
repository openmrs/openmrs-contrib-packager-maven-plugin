package org.openmrs.maven.plugins.packager.distro;

import java.io.File;
import java.io.FileInputStream;
import java.util.Properties;

import org.junit.Assert;
import org.junit.Test;

public class BuildDistroPropertiesTest {

	DistroProject distroProject;

	@Test
	public void testDistroPropertiesBuilt() throws Exception {
		distroProject = new DistroProject("distro-test");
		distroProject.executeGoal("clean", "-N", "-X");
		distroProject.executeGoal("package", "-N", "-X");

		File distroProps = distroProject.testFileExists("sources/openmrs-distro.properties");
		Properties p = new Properties();
		p.load(new FileInputStream(distroProps));
		Assert.assertEquals(8, p.size());
		Assert.assertEquals("Test Distro", p.getProperty("name"));
		Assert.assertEquals("1.0.0-SNAPSHOT", p.getProperty("version"));
		Assert.assertEquals("1.9.11", p.getProperty("war.openmrs"));
		Assert.assertEquals("1.2", p.getProperty("omod.calculation"));
		Assert.assertEquals("0.2.14", p.getProperty("omod.serialization.xstream"));
		Assert.assertEquals("omod", p.getProperty("omod.serialization.xstream.type"));
		Assert.assertEquals("1.2", p.getProperty("owa.openmrs-owa-sysadmin"));
		Assert.assertEquals("false", p.getProperty("db.h2.supported"));
	}

}
