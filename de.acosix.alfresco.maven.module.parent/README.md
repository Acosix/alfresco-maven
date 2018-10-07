[![Build Status](https://travis-ci.org/Acosix/alfresco-maven.svg?branch=master)](https://travis-ci.org/Acosix/alfresco-maven)

# About
This defines the default parent POM for Alfresco extension projects by Acosix GmbH, and more specific parent POMs for various Alfresco versions. It is meant to pre-configure most of the dependencies relevant for development in Content Services and Share, as well as pre-configure a common build process resulting in JAR and AMP packaged modules.

# Supported Alfresco Versions

Due to the varying differences between Alfresco versions and their quality of Maven artifacts, it has become difficult to provide a common parent POM that works across these different versions. In order to deal with the varying differences, dependency artifacts that have been removed from https://artifacts.alfresco.com or other peculiarities, Acosix uses specialised variants of this parent POM in actual extension projects.

The following Alfresco versions are currently supported via the following parent POMs:

* Alfresco 4.2.f - POM coordinates: de.acosix.alfresco.maven:de.acosix.alfresco.maven.module.parent-4.2.f:&lt;version&gt;
* Alfresco 5.0.d - POM coordinates: de.acosix.alfresco.maven:de.acosix.alfresco.maven.module.parent-5.0.d:&lt;version&gt;
* Alfresco 5.2.g (Repository 5.2.g and Share 5.2.f) - POM coordinates: de.acosix.alfresco.maven:de.acosix.alfresco.maven.module.parent-5.2.g:&lt;version&gt;