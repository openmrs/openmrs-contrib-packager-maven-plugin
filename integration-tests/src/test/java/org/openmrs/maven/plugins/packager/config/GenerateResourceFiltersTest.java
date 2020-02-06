package org.openmrs.maven.plugins.packager.config;

import java.io.File;
import java.io.FileInputStream;
import java.util.Properties;

import org.junit.Assert;
import org.junit.Test;

public class GenerateResourceFiltersTest {

	@Test
	public void testYmlFilesParseIntoPropertiesCorrectly() throws Exception {
		ConfigProject configProject = new ConfigProject("config-test-parent");
		configProject.executeGoal("clean", "-N", "-X");
		configProject.executeGoal("compile", "-N", "-X");

		File generatedPropertiesFile = configProject.testFileExists("configuration/constants.properties");
		Properties p = new Properties();
		p.load(new FileInputStream(generatedPropertiesFile));

		Assert.assertEquals("textValue", p.get("textConstant"));
		Assert.assertEquals("propertyValue1", p.get("constantWithProperties.property1"));
		Assert.assertEquals("propertyValue2", p.get("constantWithProperties.property2"));
		Assert.assertEquals("propertyValue3Value", p.get("constantWithProperties.property3.propertyValue3Key"));
		Assert.assertEquals("arrayValue1", p.get("constantArray[0]"));
		Assert.assertEquals("arrayValue2", p.get("constantArray[1]"));
		Assert.assertEquals("arrayValue3", p.get("constantArray[2]"));
	}

	@Test
	public void testPropertiesFilesLoadedCorrectly() throws Exception {
		ConfigProject parentProject = new ConfigProject("config-test-parent");
		parentProject.executeGoal("clean", "-N", "-X");
		parentProject.executeGoal("install", "-N", "-X");

		ConfigProject childProject = new ConfigProject("config-test-child");
		childProject.executeGoal("clean", "-N", "-X");
		childProject.executeGoal("compile", "-N", "-X");

		File generatedPropertiesFile = childProject.testFileExists("configuration/constants.properties");
		Properties p = new Properties();
		p.load(new FileInputStream(generatedPropertiesFile));

		Assert.assertEquals(8, p.size());
		Assert.assertEquals("testConstantValue", p.get("testConstantProperty"));
	}
}
