package org.openmrs.maven.plugins.packager.config;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;

import java.util.ArrayList;
import java.util.List;

import org.apache.maven.plugin.MojoExecutionException;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.openmrs.module.initializer.validator.Validator;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

@RunWith(PowerMockRunner.class)
@PrepareForTest(Validator.class)
public class ValidateConfigurationsTest {

	@Before
	public void before() {
		PowerMockito.mockStatic(Validator.class);
	}
	
	@Ignore
	@Test
	public void testConfigurationsValidated() throws Exception {
		ConfigProject configProject = new ConfigProject("config-test-parent");
		configProject.executeGoal("clean", "-N", "-X");
		configProject.executeGoal("validate", "-N", "-X");
		
		PowerMockito.verifyStatic(Validator.class, times(1));
		Validator.getJUnitResult(any(String[].class)); // TODO: make a better assert
	}
	
	@Test
	public void addValidatorCliOptions_shouldParseExtraValidatorArgs() throws MojoExecutionException {
		// setup
		String extraArgs = "--opt1 --opt2='foo bar' --opt3=bar --unsafe --ciel-file='/path/to/ciel.sql' --config-dir='/path/to/config_dir'";
		List<String> args = new ArrayList<>();
		
		// replay
		new ValidateConfigurationsMojo().addValidatorCliArguments(extraArgs, args);
		
		// verify
		assertThat(args.size(), is(3));
		assertThat(args, containsInAnyOrder("--opt1", "--opt2='foo bar'", "--opt3=bar"));
	}
}
