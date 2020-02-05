package org.openmrs.maven.plugins.packager.config;

import java.io.File;
import java.io.FileInputStream;
import java.util.Properties;

import junit.framework.TestCase;
import org.junit.Assert;

public class GenerateResourceFiltersTest extends TestCase {

	/**
	 * @throws Exception
	 */
	public void testYmlFilesHandledCorrectly() throws Exception {
		ConfigProject configProject = new ConfigProject("config-test-parent");
		configProject.executeGoal("clean", "-N", "-X");
		configProject.executeGoal("compile", "-N", "-X");

		File generatedPropertiesFile = configProject.getBuildFile("constants.properties");
		configProject.testFileExists(generatedPropertiesFile);
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
}
