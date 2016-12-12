[![Build Status](https://travis-ci.org/Acosix/alfresco-maven.svg?branch=master)](https://travis-ci.org/Acosix/alfresco-maven)

# About
This project defines the Maven build framework used by Acosix GmbH for building Alfresco-related libraries and modules.

Acosix GmbH deliberately does not use the [Alfresco SDK](https://github.com/Alfresco/alfresco-sdk) which contains too many assumptions / opinions. The latest version (3.0 at the time of writing) even hard-wires some of the assumptions into plugin behaviour with little configurability. Earlier versions regularly caused side effects with standard Maven plugins related to source code or JavaDoc attachments. This makes it hard to customize / adapt SDK-based projects to specific requirements or simply different patterns of use - even regarding something simple as custom resource directories.

# Use in projects

TBD

## Using SNAPSHOT builds

In order to use a pre-built SNAPSHOT artifact published to the Open Source Sonatype Repository Hosting site, the artifact repository may need to be added to the POM, global settings.xml or an artifact repository proxy server. The following is the XML snippet for inclusion in a POM file.

```xml
<repositories>
    <repository>
        <id>ossrh</id>
        <url>https://oss.sonatype.org/content/repositories/snapshots</url>
        <snapshots>
            <enabled>true</enabled>
        </snapshots>
    </repository>
</repositories>
```