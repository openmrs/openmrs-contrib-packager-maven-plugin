package org.openmrs.maven.plugins.packager.config;

import java.io.File;
import java.util.Arrays;

import junit.framework.TestCase;
import org.apache.maven.it.Verifier;
import org.apache.maven.it.util.ResourceExtractor;

public class PackagerConfigTest extends TestCase {

	/**
	 * @see junit.framework.TestCase#setUp()
	 */
	protected void setUp() throws Exception {
		super.setUp();
	}

	/**
	 * @throws Exception
	 */
	public void testPackagerConfig() throws Exception {

		// Set up and install test project that represents a configuration "dependency"
		Verifier parentVerifier = createVerifier("config-test-parent");
		parentVerifier.setCliOptions(Arrays.asList("-N"));
		parentVerifier.executeGoal("install");

		// Set up and install test project that represents a configuration that depends on the previous one
		Verifier childVerifier = createVerifier("config-test-child");
		childVerifier.setCliOptions(Arrays.asList("-N"));
		childVerifier.executeGoal("compile");


	}

	protected Verifier createVerifier(String artifact) throws Exception {
		File resource = ResourceExtractor.simpleExtractResources(getClass(), "/" + artifact);
		Verifier verifier = new Verifier(resource.getAbsolutePath());
		verifier.deleteArtifacts("org.openmrs.maven.plugins.openmrs-packager-maven-plugin-" + artifact);
		return verifier;
	}
}
