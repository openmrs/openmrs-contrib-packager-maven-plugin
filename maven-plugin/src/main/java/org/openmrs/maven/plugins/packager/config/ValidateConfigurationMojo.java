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

import static org.openmrs.module.initializer.validator.Validator.ARG_CONFIG_DIR;

import java.io.File;
import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.List;

import javax.persistence.Cache;
import javax.persistence.EntityGraph;
import javax.persistence.PersistenceException;
import javax.persistence.PersistenceUnitUtil;
import javax.persistence.Query;
import javax.persistence.TransactionRequiredException;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.metamodel.Metamodel;

import org.apache.commons.cli.ParseException;
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
@Mojo( name = "validate-configuration" )
public class ValidateConfigurationMojo extends AbstractPackagerConfigMojo {
	
	// Configuration Directory
	@Parameter(property = "sourceDir", defaultValue = "configuration")
	private File sourceDir;
	
	private void findClasses() {
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
	 * @throws MojoExecutionException if an error occurs
	 */
	public void execute() throws MojoExecutionException {
		
		findClasses();
		
		if (sourceDir == null || !sourceDir.isDirectory()) {
			throw new MojoExecutionException(sourceDir.getAbsolutePath() + " does not point to a valid directory.");
		}
		
		List<String> args = Arrays.asList("--" + ARG_CONFIG_DIR + "=" + sourceDir.getAbsolutePath());
		
		Result result = new Result();
		try {
			result = Validator.getJUnitResult(args.toArray(new String[0]));
		}
		catch (URISyntaxException | ParseException e) {
			getLog().error(e.getMessage(), e);
		}
		finally {
			if (!result.wasSuccessful()) {
				throw new MojoExecutionException("The configuration could not be validated, scroll up the Maven build logs for error details.");
			}
		}
		
	}
	
}
