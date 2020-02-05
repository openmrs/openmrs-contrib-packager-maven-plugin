package org.openmrs.maven.plugins.packager.config;

import java.io.File;
import java.util.Arrays;

import org.apache.commons.io.FileUtils;
import org.apache.maven.it.Verifier;
import org.apache.maven.it.util.ResourceExtractor;
import org.junit.Assert;

public class ConfigProject {

	private String configDirName;
	private File configDir;
	private Verifier verifier;
	private File targetDir;
	private File pluginBuildDir;

	/**
	 * Sets up a new test config project using the given config directory as the simulated project
	 * Ensures that a verifier exists that can be used for this project
	 */
	public ConfigProject(String configDirName) {
		this.configDirName = configDirName;
		try {
			configDir = ResourceExtractor.simpleExtractResources(getClass(), "/" + configDirName);
			verifier = new Verifier(configDir.getAbsolutePath());
			targetDir = new File(configDir, "target");
			pluginBuildDir = new File(targetDir, "openmrs-packager-config");
			verifier.deleteArtifacts("org.openmrs.maven.plugins.openmrs-packager-maven-plugin-" + configDirName);
		}
		catch (Exception e) {
			throw new IllegalStateException("Unable to setup Config Project from " + configDirName, e);
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

	public File getBuildFile(String path) {
		return new File(pluginBuildDir, path);
	}

	public void testFileExists(File file) {
		Assert.assertTrue("Checking that " + file + " exists", file.exists());
	}

	public void testFileContains(File file, String contents) throws Exception {
		Assert.assertTrue("Checking that " + file + " exists", file.exists());
		String existing = FileUtils.readFileToString(file, "UTF-8");
		Assert.assertTrue(existing.contains(contents));
	}

	//***** PROPERTY ACCESSORS

	public String getConfigDirName() {
		return configDirName;
	}

	public void setConfigDirName(String configDirName) {
		this.configDirName = configDirName;
	}

	public File getConfigDir() {
		return configDir;
	}

	public void setConfigDir(File configDir) {
		this.configDir = configDir;
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
