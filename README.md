# Branch Maven Plugin

Based on the infrastructure provided by the Versions plugin (see below), the Branch plugin rewrites SNAPSHOT versions (for both projects and dependencies) to include a branch name passed in as parameter. These artifacts can then be deployed to a Maven repo without any risk of interference.

The plugin would typically be invoked in the CI environment (Jenkins) before building the project. When invoked from Jenkins, the branch is available as `$GIT_BRANCH`.
 
```
mvn com.clearcapital.maven.plugins:cc-branch-maven-plugin:1.0:branch -D branch=origin/CCP-1234
```

Below is the original description of the Versions plugin.

# MojoHaus Versions Maven Plugin

This is the [versions-maven-plugin](http://www.mojohaus.org/versions-maven-plugin/).
 
[![Apache License, Version 2.0, January 2004](https://img.shields.io/github/license/mojohaus/versions-maven-plugin.svg?label=License)](http://www.apache.org/licenses/)
[![Maven Central](https://img.shields.io/maven-central/v/org.codehaus.mojo/versions-maven-plugin.svg?label=Maven%20Central)](http://search.maven.org/#search%7Cgav%7C1%7Cg%3A%22org.codehaus.mojo%22%20AND%20a%3A%22versions-maven-plugin%22)
[![Build Status](https://travis-ci.org/mojohaus/versions-maven-plugin.svg?branch=master)](https://travis-ci.org/mojohaus/versions-maven-plugin)

## Releasing

* Make sure `gpg-agent` is running.
* Execute `mvn -B release:prepare release:perform`

For publishing the site do the following:

```
cd target/checkout
mvn verify site site:stage scm-publish:publish-scm
```
