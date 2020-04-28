package org.openmrs.maven.plugins.packager.distro;

import java.io.File;
import java.util.Arrays;

import org.apache.commons.io.FileUtils;
import org.apache.maven.it.Verifier;
import org.apache.maven.it.util.ResourceExtractor;
import org.junit.Assert;

public class DistroProject {

	private String srcDirName;
	private File srcDir;
	private Verifier verifier;
	private File targetDir;
	private File pluginBuildDir;

	/**
	 * Sets up a new test config project using the given config directory as the simulated project
	 * Ensures that a verifier exists that can be used for this project
	 */
	public DistroProject(String srcDirName) {
		this.srcDirName = srcDirName;
		try {
			srcDir = ResourceExtractor.simpleExtractResources(getClass(), "/" + srcDirName);
			verifier = new Verifier(srcDir.getAbsolutePath());
			targetDir = new File(srcDir, "target");
			pluginBuildDir = new File(targetDir, "openmrs-packager-distro");
			verifier.deleteArtifacts("org.openmrs.maven.plugins.openmrs-packager-maven-plugin-" + srcDirName);
		}
		catch (Exception e) {
			throw new IllegalStateException("Unable to setup Config Project from " + srcDirName, e);
		}
	}

	/**
	 * Executes the specified mvn goal on the config project, with optional command line options
	 */
	public void executeGoal(String goal, String... cliOptions) throws Exception {
		if (cliOptions.length > 0) {
			verifier.setCliOptions(Arrays.asList(cliOptions));
		}
		verifier.executeGoal(goal);
	}

	public File testFileExists(String path) {
		File file = new File(pluginBuildDir, path);
		Assert.assertTrue("Checking that " + file + " exists", file.exists());
		return file;
	}

	public void testFileDoesNotExist(String path) {
		File file = new File(pluginBuildDir, path);
		Assert.assertFalse("Checking that " + file + " does not exist", file.exists());
	}

	public void testFileContains(File file, String contents) throws Exception {
		Assert.assertTrue("Checking that " + file + " exists", file.exists());
		String existing = FileUtils.readFileToString(file, "UTF-8");
		Assert.assertTrue(existing.contains(contents));
	}

	//***** PROPERTY ACCESSORS

	public String getSrcDirName() {
		return srcDirName;
	}

	public void setSrcDirName(String srcDirName) {
		this.srcDirName = srcDirName;
	}

	public File getSrcDir() {
		return srcDir;
	}

	public void setSrcDir(File srcDir) {
		this.srcDir = srcDir;
	}

	public Verifier getVerifier() {
		return verifier;
	}

	public void setVerifier(Verifier verifier) {
		this.verifier = verifier;
	}

	public File getTargetDir() {
		return targetDir;
	}

	public void setTargetDir(File targetDir) {
		this.targetDir = targetDir;
	}

	public File getPluginBuildDir() {
		return pluginBuildDir;
	}

	public void setPluginBuildDir(File pluginBuildDir) {
		this.pluginBuildDir = pluginBuildDir;
	}
}
