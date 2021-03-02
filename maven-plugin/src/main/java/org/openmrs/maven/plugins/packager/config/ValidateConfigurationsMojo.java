/**
 * This Source Code Form is subject to the terms of the Mozilla Public License,
 * v. 2.0. If a copy of the MPL was not distributed with this file, You can
 * obtain one at http://mozilla.org/MPL/2.0/. OpenMRS is also distributed under
 * the terms of the Healthcare Disclaimer located at http://openmrs.org/license.
 *
 * Copyright (C) OpenMRS Inc. OpenMRS is a registered trademark and the OpenMRS
 * graphic logo is a trademark of OpenMRS Inc.
 */
package org.openmrs.maven.plugins.packager.config;

import static org.apache.commons.lang.StringUtils.isEmpty;
import static org.openmrs.module.initializer.validator.Validator.ARG_CIEL_FILE;
import static org.openmrs.module.initializer.validator.Validator.ARG_CONFIG_DIR;
import static org.openmrs.module.initializer.validator.Validator.ARG_UNSAFE;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import javax.persistence.Cache;
import javax.persistence.EntityGraph;
import javax.persistence.PersistenceException;
import javax.persistence.PersistenceUnitUtil;
import javax.persistence.Query;
import javax.persistence.TransactionRequiredException;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.metamodel.Metamodel;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.junit.runner.Result;
import org.openmrs.module.initializer.validator.Validator;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.orm.jpa.EntityManagerFactoryUtils;
import org.springframework.orm.jpa.JpaObjectRetrievalFailureException;
import org.springframework.orm.jpa.JpaOptimisticLockingFailureException;
import org.springframework.orm.jpa.JpaSystemException;

/**
 * The purpose of this Mojo is to validate configurations.
 */
@Mojo( name = "validate-configurations" )
public class ValidateConfigurationsMojo extends AbstractPackagerConfigMojo {
	
	// Configuration Directory
	@Parameter(property = "sourceDir", defaultValue = "configuration")
	private File sourceDir;
	
	@Parameter(property = "cielFile")
	private File cielFile;
	
	// Extra Validator CLI options
	@Parameter(property = "extraValidatorArgs")
	private String extraValidatorArgs;
	
	protected File getSourceDir() {
		return sourceDir;
	}
	
	/*
	 * To avoid NoClassDefFoundError on EntityManagerFactoryUtils and related classes.
	 */
	private void findClassDefinitions() {
		EntityManagerFactoryUtils.class.toString();
		PersistenceException.class.toString();
		TransactionRequiredException.class.toString();
		JpaObjectRetrievalFailureException.class.toString();
		EmptyResultDataAccessException.class.toString();
		JpaOptimisticLockingFailureException.class.toString();
		JpaSystemException.class.toString();
		Cache.class.toString();
		CriteriaBuilder.class.toString();
		Metamodel.class.toString();
		PersistenceUnitUtil.class.toString();
		Query.class.toString();
		EntityGraph.class.toString();
	}
	
	/**
	 * @throws MojoExecutionException if the Maven build should be errored.
	 */
	public void execute() throws MojoExecutionException {
		
		findClassDefinitions(); // TODO: figure out why this is needed
		
		List<String> args = new ArrayList<>();
		if (getSourceDir() == null || !getSourceDir().isDirectory()) {
			throw new MojoExecutionException(getSourceDir().getAbsolutePath() + " does not point to a valid directory.");
		}
		args.add("--" + ARG_CONFIG_DIR + "=" + getSourceDir().getAbsolutePath());
		
		if (cielFile != null) {
			args.add("--" + ARG_CIEL_FILE + "=" + cielFile.getAbsolutePath());
		}
		
		if (!isEmpty(extraValidatorArgs)) {
			addValidatorCliArguments(extraValidatorArgs, args);
		}
		
		Result result = new Result();
		try {
			args.add("--" + ARG_UNSAFE);
			result = Validator.getJUnitResult(args.toArray(new String[0]));
		}
		catch (Exception e) {
			throw new MojoExecutionException(e.getMessage(), e);
		}
		
		if (!result.wasSuccessful()) {
			throw new MojoExecutionException("The configuration could not be validated, scroll up the Maven build logs for details.");
		}
		
	}
	
	/**
	 * Parses a one liner string of Validators arguments into a list of arguments supported by the plugin. 
	 * 
	 * @param opts The string Validators args/options, eg. "--domains='concepts,locations' --exclude.concepts='*diags*,*interventions*'"
	 * @param args (ouput) The list of Validator args.
	 */
	protected void addValidatorCliArguments(String opts, List<String> args) {
		Stream.of(opts.split("--")).map(o -> o.trim()).filter(o -> !isEmpty(o)).filter(o -> includeOption(o)).forEach(o -> {
			args.add("--" + o);
		});
	}
	
	protected boolean includeOption(String opt) {
		if (opt.startsWith(ARG_CONFIG_DIR)) {
			getLog().warn("--" + ARG_CONFIG_DIR + " cannot be provided as an extra Validator argument, use <sourceDir/> instead in the plugin configuration.");
			return false;
		}
		if (opt.startsWith(ARG_CIEL_FILE)) {
			getLog().warn("--" + ARG_CIEL_FILE + " cannot be provided as an extra Validator argument, use <cielFile/> instead in the plugin configuration.");
			return false;
		}
		if (opt.startsWith(ARG_UNSAFE)) {
			getLog().info("--" + ARG_UNSAFE + " is redundant since the plugin always validates configurations in unsafe mode.");
			return false;
		}
		return true;
	}
	
}
