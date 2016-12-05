[![Build Status](https://travis-ci.org/Acosix/alfresco-maven.svg?branch=master)](https://travis-ci.org/Acosix/alfresco-maven)

# About
This project defines the Maven build framework used by Acosix GmbH for building Alfresco-related libraries and modules.

Acosix GmbH deliberately does not use the [Alfresco SDK](https://github.com/Alfresco/alfresco-sdk) which contains too many assumptions / opinions. The latest version (3.0 at the time of writing) even hard-wires some of the assumptions into plugin behaviour with little configurability. Earlier versions regularly caused side effects with standard Maven plugins related to source code or JavaDoc attachments. This makes it hard to customize / adapt SDK-based projects to specific requirements or simply different patterns of use - even regarding something simple as custom resource directories.