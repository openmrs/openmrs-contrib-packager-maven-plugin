openmrs-packager-maven-plugin
==============================

The goal of this project is to provide capabilities within Maven that minimizes the boilerplate xml configuration for
OpenMRS projects, and increases the standardization of approach as a result.

### Configuration Packaging

A core use case for this plug-in is to enable packaging up an "OpenMRS configuration" project
into a standard file structure within a zip artifact that is suitable to be deployed into an OpenMRS distribution.
This configuration is intended to be utilized by modules during the server start-up process to set up required configurations and metadata

#### High-level project structure

An configuration project wishing to use this plug-in should adhere to the following structure
```
openmrs-config-sample
  pom.xml
  constants.yml
  dependencies.yml
  configuration/
```

#### Top-level POM

The top-level POM serves two purposes:

1. To identify the artifact and version for packaging and deployment.
```xml
<project>
  <modelVersion>4.0.0</modelVersion>
  <groupId>your-group-id</groupId>
  <artifactId>openmrs-config-your-configuration-id</artifactId>
  <name>your name</name>
  <description>your description</description>
  <version>1.0.0-SNAPSHOT</version>
  <packaging>pom</packaging>
</project>
```

2. To define the usage of this plugin for packaging.  The below should be copied exactly after the packaging element above:

```xml
  <build>
    <filters>
      <filter>${project.build.directory}/openmrs-packager-config/constants.properties</filter>
    </filters>
    <plugins>
      <plugin>
        <groupId>org.openmrs.maven.plugins</groupId>
        <artifactId>openmrs-packager-maven-plugin</artifactId>
        <version>1.0-SNAPSHOT</version>
        <executions>
          <execution>
            <id>generate-resource-filters</id>
            <phase>generate-resources</phase>
            <goals>
              <goal>generate-resource-filters</goal>
            </goals>
          </execution>
          <execution>
            <id>compile-configurations</id>
            <phase>compile</phase>
            <goals>
              <goal>compile-configurations</goal>
            </goals>
          </execution>
          <execution>
            <id>package-configurations</id>
            <phase>package</phase>
            <goals>
              <goal>package-configurations</goal>
            </goals>
          </execution>
          <execution>
            <id>validate-configurations</id>
            <phase>validate</phase>
            <goals>
              <goal>validate-configurations</goal>
            </goals>
          </execution>
        </executions>
      </plugin>
    </plugins>
  </build>
```

#### Adding Configuration Files

All configuration files that you wish to have packaged up should be placed in a top-level "configuration" directory.  This contents
of this directory will be added directly to the built zip artifact minus this enclosing directory.  So if there is a directory structure:
`configuration/ordertypes/ordertypes.csv`
The resulting zip archive will contain:
`ordertypes/ordertypes.csv`

#### Supporting constants and variable replacement

Most complex configurations within OpenMRS will require using the same fixed values across multiple configurations.  This can
quickly become difficult to manage and support, and can lead to hard-to-track bugs.  A common example might have the uuid of a
particular encounter type configured in global properties, application configs, report definitions, and htmlforms.  By supporting
a constants mechanism, we allow configuation managers to extract these common values into a single file, and then refer to them
by reference throughout the rest of their configurations.  There are two supported constant files that can be included
at the top level of the project.  One can choose to include one or the other (or neither) but not both.

**/constants.properties** - Any property defined here can be referred to with the syntax: `${propertyName}`

**/constants.yml** - This provides richer syntax to properties that some might find preferable, particularly if there are 
deeply nested configuration settings, or for easier namespacing.  
Nested properties are referenced via "dot notation".  Lists are referenced via "index" notation.

For example, the following constants.yml file:

```yaml
favoriteColor: "blue"

radiologyTestOrderType:
  uuid: "5a3a8d2e-97c3-4797-a6a8-5417e6e699ec"
  names: 
    en: "Radiology Order"`
```

Will yield the following available replacements:

```
${favoriteColor}
${radiologyTestOrderType.uuid}
${radiologyTestOrderType.names.en}
```

#### Supporting dependencies

One of the primary reasons for using Maven, and for this plugin, is for the support for dependencies.
This allows several independent configuration projects to emerge that might need to evolve differently.  The most common
example of this would be to have a configuration for one or more implementations that need to share and independently extend
a set of common configurations.  This has the benefit of enabling a given top-level implementation config to depend on 
specific versions of dependent configuration artifacts, and to override and extend these as needed.

In order to declare a dependency, you would do so within a top-level "dependencies.yml" file, that supports the following structure:

**dependencies.yml**
```yaml
- groupId: "org.pih.emr"
  artifactId: "openmrs-config-mvnparent"
  version: "1.0.0-SNAPSHOT"`
```

All dependencies are expected to follow the same packaging conventions as defined here.  A zip file is expected
that contains all configuration artifacts, and are copied into the resulting zip directly.

In order to support overriding values, one should specify their dependencies in the order that they wish them to be loaded.

The first dependency listed in dependencies.yml will have any files with matching names overridden by the second listed dependency,
which will subsequently be overridden by the third listed dependency, and so forth.  The last thing copied in are the configurations
maintained within the defining project, which will provide any final additions and file overrides.

There is no current support for merging files, nor for installing a subset of configurations for a given dependency.

#### Usage

`mvn clean compile` - Will generate your configurations into "target/openmrs-packager-config/configuration"
`mvn clean package` - Will compile as above, and generate a zip package at "target/${artifactId}-${version}.zip"
`mvn clean install` - Will compile and package as above, and install as an available dependency on your system

The install goal is necessary if you are working on a dependency and wish for the latest changes to be reflected in a 
"downstream" configuration package.

In order to facilitate deploying configurations easily into an OpenMRS SDK server, one can add an additional parameter
to either of the above commands to specify that the compiled configuration should also be copied to an existing 
OpenMRS SDK server:

`mvn clean compile -DserverId=wellbody` - Will compile as above, and copy the resulting configuration to `~/openmrs/wellbody/configuration`

In order to facilitate developing configuration such as htmlforms or other UI configurations without having to manually
recompile constantly to view changes, there is also a watch option that will continuously redeploy to your specified server.
If any file change is detected, this will automatically compile and copy the configurations over to your SDK server.  

To enable watching, you run the following:

`mvn clean openmrs-packager:watch`

This is hard coded to watch any files within your project for changes, ignoring the build directory (target) and the .git
directories.

In order to not continuously deploy as changes are actively being made, there is a built-in delay of 5 seconds between the
last file modification time, and the time at which the watch will execute.  This delay is configurable to any whole
number via runtime configuration "delaySeconds":

`mvn clean openmrs-packager:watch -DdelaySeconds=1`

By default, when the watch executes each time, it will run the "install" goal, which compiles, packages (zips), and saves
the built configuration.  To change this goal, you can do with via the "goal" runtime configuration parameter:

`mvn clean openmrs-packager:watch -Dgoal=compile`

This also allows you to deploy to an OpenMRS server id upon each change.  Simply append the "serverId" runtime
parameter as defined above.

These can all be included as desired, for example:

`mvn clean openmrs-packager:watch -Dgoal=compile -DserverId=wellbody -DdelaySeconds=3`

Watchers not only watch the configuration files they define, but also watch for changes to their dependent libraries.
This allows for useful "chaining" of watchers.

For example, if you have a parent project that you depend upon, you could watch it for changes:

From "openmrs-config-parent" directory:
`mvn clean openmrs-packager:watch`

It is important to note that no serverId property was specified on the parent project.  This is not copying directly to
the server, but is installing new package versions that the child project can detect.

From the "openmrs-config-child" directory:
`mvn clean openmrs-packager:watch -DserverId=wellbody`

Here is where the serverId is specified, and this will watch for any configuration changes made directly to the child
project, and _also_ will watch for any changes in the parent project, by way of watching the directory into which new 
versions of the parent configuration artifact is installed.

The result of the above is that you can have a hierarchy of configurations that depend upon one another and all
of which automatically build and deploy to an openmrs server during development.

#### TODO

* Add more sophistication to dependencies
  * Include resources that match particular patterns or directories
  * Ability to merge/overwrite within a file, rather that overwrite files completely
  
* Add ability to have/support more than 1 constant file (figure out if we can define these resources programmatically rather than only via xml)
  
* Extend to packaging distributions (war, omod, config, other resources)

* Extend to building omods (eliminate boilerplate and gain consistency in module pom.xml code)

* Add useful validations
  * HtmlForms?
  * Concepts and other Metadata?

* Add ability to generate code/configurations
  * Generate global property configuration from constants.yml
  * Generate jsonKeyValue configuration from constants.yml
  * Generate initializer CSV files from yml constant files

* Additional unit and integration tests
