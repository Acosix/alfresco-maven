/*
 * Copyright 2016 Acosix GmbH
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package de.acosix.alfresco.maven.plugins.i18n;

import java.io.File;
import java.io.IOException;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.codehaus.plexus.util.DirectoryScanner;
import org.codehaus.plexus.util.FileUtils;

/**
 * @author Axel Faust, <a href="http://acosix.de">Acosix GmbH</a>
 *
 * @goal duplicateI18nResources
 * @phase generate-sources
 * @requiresProject
 * @threadSafe
 * @description Duplicates existing resource bundles to provide basic support for locales without falling back to the default locale
 *              bundles.
 */
public class DuplicateI18nResourcesMojo extends AbstractMojo
{

    private static final String PROPERTIES_EXTENSION = ".properties";

    protected static final List<String> DEFAULT_INCLUDES = Collections.unmodifiableList(Arrays.asList("messages/**/*.properties",
            "webscripts/**/*.properties", "site-webscripts/**/*.properties", "templates/**/*.properties", "webapp/**/*.properties"));

    /**
     * @parameter default-value="${basedir}/src/main"
     * @required
     */
    protected File sourceDirectory;

    /**
     * @parameter default-value="${project.build.directory}/i18n-resources"
     * @required
     */
    protected File outputDirectory;

    /**
     * @parameter
     */
    protected List<String> includes;

    /**
     * @parameter
     */
    protected List<String> excludes;

    /**
     * @parameter
     */
    protected String sourceLocale;

    /**
     * @parameter default-value="en"
     * @required
     */
    protected String targetLocale;

    /**
     * {@inheritDoc}
     */
    @Override
    public void execute() throws MojoExecutionException, MojoFailureException
    {
        final List<String> propertyFileNames = getPropertyFileNamesToProcess();
        for (final String propertyFileName : propertyFileNames)
        {
            if (propertyFileName.endsWith(PROPERTIES_EXTENSION))
            {
                final String fileName = propertyFileName.indexOf('/') != -1
                        ? propertyFileName.substring(propertyFileName.lastIndexOf('/') + 1) : propertyFileName;
                final String relativePath = propertyFileName.indexOf('/') != -1
                        ? propertyFileName.substring(0, propertyFileName.lastIndexOf('/') + 1) : "";
                final String fileBaseName = fileName.substring(0, fileName.length() - PROPERTIES_EXTENSION.length());

                final StringBuilder localeBuilder = new StringBuilder();
                String resourceName = fileBaseName;
                while (resourceName.lastIndexOf('_') == resourceName.length() - 3 && localeBuilder.length() < 8)
                {
                    final String fragment = resourceName.substring(resourceName.lastIndexOf('_') + 1);
                    ;
                    resourceName = resourceName.substring(0, resourceName.lastIndexOf('_'));
                    if (localeBuilder.length() != 0)
                    {
                        localeBuilder.insert(0, '_');
                    }
                    localeBuilder.insert(0, fragment);
                }

                if ((this.sourceLocale == null && localeBuilder.length() == 0)
                        || (this.sourceLocale != null && this.sourceLocale.equals(localeBuilder.toString())))
                {
                    final String targetResourceName = this.targetLocale.trim().length() > 0
                            ? MessageFormat.format("{0}_{1}", resourceName, this.targetLocale) : resourceName;
                    final String targetFileName = targetResourceName + PROPERTIES_EXTENSION;
                    final String targetPropertyFileName = relativePath + targetFileName;

                    final File target = new File(this.outputDirectory, targetPropertyFileName);
                    target.getParentFile().mkdirs();
                    try
                    {
                        FileUtils.copyFile(new File(this.sourceDirectory, propertyFileName), target);
                    }
                    catch (final IOException ioEx)
                    {
                        throw new MojoExecutionException("Error copying resources", ioEx);
                    }
                }
            }
        }
    }

    protected List<String> getPropertyFileNamesToProcess()
    {
        final List<String> propertyFileNames = new ArrayList<>();

        final DirectoryScanner scanner = new DirectoryScanner();
        scanner.setBasedir(this.sourceDirectory);

        if (this.includes != null)
        {
            scanner.setIncludes(this.includes.toArray(new String[0]));
        }
        else
        {
            scanner.setIncludes(DEFAULT_INCLUDES.toArray(new String[0]));
        }

        if (this.excludes != null)
        {
            scanner.setExcludes(this.excludes.toArray(new String[0]));
        }

        scanner.addDefaultExcludes();
        scanner.scan();

        for (final String includedFile : scanner.getIncludedFiles())
        {
            propertyFileNames.add(includedFile);
        }

        return propertyFileNames;
    }
}
