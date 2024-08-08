
package org.openmrs.maven.plugins.packager.config;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.FileInputStream;
import java.util.Properties;

import org.apache.maven.plugin.MojoExecutionException;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

public class ValidateContentPackageMojoTest {
	
	@InjectMocks
	private ValidateContentPackageMojo mojo = new ValidateContentPackageMojo();
	
	@Mock
	private FileInputStream mockFileInputStream;
	
	@Mock
	private Properties mockProperties;
	
	@Rule
	public ExpectedException exceptionRule = ExpectedException.none();
	
	@Before
	public void setUp() {
		MockitoAnnotations.initMocks(this);
	}
	
	@Test
	public void executeValidContentPropertiesFileWithoutErrors() throws Exception {
		String validPropertiesFile = "src/test/resources/config-test-child/valid-content.properties";
		mojo.sourceFile = validPropertiesFile;
		mojo.execute();
		//no exception is thrown
	}
	
	@Test(expected = MojoExecutionException.class)
	public void executeInvalidContentPropertiesFormatWithException() throws Exception {
		String invalidPropertiesFile = "src/test/resources/config-test-child/invalid-content.properties";
		mojo.sourceFile = invalidPropertiesFile;
		mojo.execute();
		//exception is thrown
	}
	
	@Test
	public void testInValidVersions() {
		assertFalse(mojo.isValid("1.0.0-1234-"));
		assertFalse(mojo.isValid("abc"));
		assertFalse(mojo.isValid("1..0"));
		assertFalse(mojo.isValid("latest"));
		assertFalse(mojo.isValid("next"));
	}
	
	@Test
	public void testValidVersionRanges() {
		assertTrue(mojo.isValid("1.0"));
		assertTrue(mojo.isValid("0.13.0"));
		assertTrue(mojo.isValid("^2.13.0"));
		assertTrue(mojo.isValid("~0.13.0"));
		assertTrue(mojo.isValid(">0.13.0"));
		assertTrue(mojo.isValid("<3.0.0"));
		assertTrue(mojo.isValid(">=3.0.0"));
		assertTrue(mojo.isValid("<=3.0.0"));
		assertTrue(mojo.isValid("1.0.0 - 1.10.10"));
		assertTrue(mojo.isValid("<2.1.0 || >2.6.0"));
		assertTrue(mojo.isValid(">=1.0.0-SNAPSHOT"));
		assertTrue(mojo.isValid(">=1.0.0-pre.1"));
	}
	
	@Test
	public void validNPMVersion() {
		assertTrue(mojo.isValid("1.0.01"));
		assertTrue(mojo.isValid("1.0.0-alpha@"));
		assertTrue(mojo.isValid("1.0.0.0"));
	}
	
	@Test
	public void testComplexRanges() {
		assertTrue(mojo.isValid(">=1.0.0 <2.0.0"));
		assertTrue(mojo.isValid("1.1.1 || 1.2.3 - 2.0.0"));
	}
}
