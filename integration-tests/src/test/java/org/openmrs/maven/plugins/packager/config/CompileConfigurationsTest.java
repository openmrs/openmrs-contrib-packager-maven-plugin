package org.openmrs.maven.plugins.packager.config;

import java.io.File;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class CompileConfigurationsTest {

	ConfigProject parentProject;
	ConfigProject childProject;

	@Before
	public void beforeEachTest() throws Exception {
		parentProject = new ConfigProject("config-test-parent");
		parentProject.executeGoal("clean", "-N", "-X");
		parentProject.executeGoal("install", "-N", "-X");

		childProject = new ConfigProject("config-test-child");
		childProject.executeGoal("clean", "-N", "-X");
		childProject.executeGoal("compile", "-N", "-X");
	}

	@Test
	public void testDependenciesAreLoaded() throws Exception {
		parentProject.testFileDoesNotExist("dependencies");
		File childDependencies = childProject.testFileExists("dependencies");
		String[] fileNames = childDependencies.list();
		Assert.assertTrue(fileNames.length > 0);
	}

	@Test
	public void testDependenciesAreMerged() throws Exception {
		childProject.testFileExists("configuration/domain1/constantsTest.xml");
		childProject.testFileExists("configuration/domain2/file-from-parent.txt");
		childProject.testFileExists("configuration/domain2/file-from-child.txt");
		childProject.testFileExists("configuration/domain3/domain-not-in-parent.txt");
	}

	@Test
	public void testVariablesAreReplaced() throws Exception {
		File xmlFile = parentProject.testFileExists("configuration/domain1/constantsTest.xml");
		parentProject.testFileContains(xmlFile, "<textConstant>textValue</textConstant>");
		parentProject.testFileContains(xmlFile, "<nestedProperty1>propertyValue1</nestedProperty1>");
		parentProject.testFileContains(xmlFile, "<nestedProperty2>propertyValue2</nestedProperty2>");
		parentProject.testFileContains(xmlFile, "<nestedProperty3>propertyValue3Value</nestedProperty3>");
		parentProject.testFileContains(xmlFile, "<arrayZero>arrayValue1</arrayZero>");
		parentProject.testFileContains(xmlFile, "<arrayOne>arrayValue2</arrayOne>");
		parentProject.testFileContains(xmlFile, "<arrayTwo>arrayValue3</arrayTwo>");
		parentProject.testFileContains(xmlFile, "<variableWithNoReplacementInParent>${missingConstant}</variableWithNoReplacementInParent>");
	}

	// TBD: Test copy to server
	// TBD: Test watch
}
