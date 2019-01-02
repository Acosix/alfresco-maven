package de.acosix.alfresco.maven.plugins.archiver;

import org.codehaus.plexus.archiver.zip.ZipArchiver;

/**
 *
 * @author Axel Faust
 */
public class AmpArchiver extends ZipArchiver
{

    public AmpArchiver()
    {
        super.archiveType = "amp";
    }
}
