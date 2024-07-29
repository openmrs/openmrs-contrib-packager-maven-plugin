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
		// setup
		String validPropertiesFile = "src/test/resources/config-test-child/valid-content.properties";
		mojo.sourceFile = validPropertiesFile;
		// replay
		mojo.execute();
		
		// verify
		// No exception should be thrown
	}
	
	@Test
	public void executeInvalidContentPropertiesFormatWithException() throws Exception {
		// setup
		String invalidPropertiesFile = "src/test/resources/config-test-child/invalid-content.properties";
		mojo.sourceFile = invalidPropertiesFile;
		
		exceptionRule.expect(MojoExecutionException.class);
		exceptionRule.expectMessage("Error validating properties file");
		
		// replay
		mojo.execute();
		
	}
	
	@Test
	public void testValidVersions() {
		assertTrue(mojo.isValidVersion("0.13.0"));
		assertTrue(mojo.isValidVersion("3.0.0"));
		assertTrue(mojo.isValidVersion("1.0.0"));
	}
	
	@Test
	public void testValidVersionRanges() {
		assertTrue(mojo.isValidVersion("^0.13.0"));
		assertTrue(mojo.isValidVersion("~0.13.0"));
		assertTrue(mojo.isValidVersion(">0.13.0"));
		assertTrue(mojo.isValidVersion("<3.0.0"));
		assertTrue(mojo.isValidVersion(">=3.0.0"));
		assertTrue(mojo.isValidVersion("<=3.0.0"));
		assertTrue(mojo.isValidVersion("=3.0.0"));
		assertTrue(mojo.isValidVersion("1.0.0 - 1.10.10"));
		assertTrue(mojo.isValidVersion("<2.1.0 || >2.6.0"));
	}
	
	@Test
	public void testInvalidVersions() {
		assertFalse(mojo.isValidVersion("abc"));
		assertFalse(mojo.isValidVersion("1..0"));
		assertFalse(mojo.isValidVersion("1.0.0.0"));
		assertFalse(mojo.isValidVersion("latest"));
		assertFalse(mojo.isValidVersion("next"));
	}
	
	@Test
	public void testPreReleaseAndBuildMetadata() {
		assertTrue(mojo.isValidVersion("1.0.0-alpha"));
		assertTrue(mojo.isValidVersion("1.0.0+20130313144700"));
		assertTrue(mojo.isValidVersion("1.0.0-beta+exp.sha.5114f85"));
	}
	
	@Test
	public void testComplexRanges() {
		assertFalse(mojo.isValidVersion(">=1.0.0 <2.0.0")); //has to be fixed
		assertTrue(mojo.isValidVersion("1.2.3 - 2.3.4"));
		assertFalse(mojo.isValidVersion(">=1.2.3 <2.3.4 || >=3.0.0")); //has to be fixed
	}
}
