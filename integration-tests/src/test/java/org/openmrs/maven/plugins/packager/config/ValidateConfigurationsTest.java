package org.openmrs.maven.plugins.packager.config;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.powermock.api.mockito.PowerMockito.doThrow;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.apache.maven.plugin.MojoExecutionException;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.Result;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.openmrs.module.initializer.validator.Validator;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

@RunWith(PowerMockRunner.class)
@PrepareForTest(Validator.class)
public class ValidateConfigurationsTest {
	
	private File logDir = Mockito.mock(File.class);
	
	private class TestMojo extends ValidateConfigurationsMojo {
		@Override
		protected File getSourceDir() {
			return new File(getClass().getClassLoader().getResource("config-test-parent/configuration").getPath());
		}
		@Override
		protected File getBuildDir() {
			return logDir;
		}
	}
	
	private ValidateConfigurationsMojo mojo = new TestMojo();
	
	private Result result;

	@Before
	public void before() {
		PowerMockito.mockStatic(Validator.class);
	}
	
	@Rule
	public ExpectedException exceptionRule = ExpectedException.none();
	
	@Test(expected = Test.None.class)
	public void execute_successfulJUnitResultShouldNotThrowMojoExecutionException() throws Exception {
		// setup
		result = new Result();
		when(Validator.getJUnitResult(any(String[].class))).thenReturn(result);
		
		// replay
		mojo.execute();
		verify(logDir, times(1)).getAbsolutePath();
	}
	
	@Test
	public void execute_failedJUnitResultShouldThrowMojoExecutionException() throws Exception {
		// setup
		result = new Result() {
			@Override
			public boolean wasSuccessful() {
		        return false;
		    }
		};
		when(Validator.getJUnitResult(any(String[].class))).thenReturn(result);
		
		// replay
		exceptionRule.expect(MojoExecutionException.class);
		mojo.execute();
	}
	
	@Test
	public void execute_throwingValidatorShouldThrowMojoExecutionException() throws Exception {
		// setup
		doThrow(new RuntimeException()).when(Validator.class);
		Validator.getJUnitResult(any(String[].class));
		
		// replay
		exceptionRule.expect(MojoExecutionException.class);
		mojo.execute();
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
