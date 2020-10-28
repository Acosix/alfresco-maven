/*
 * Copyright 2016 - 2020 Acosix GmbH
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
package de.acosix.alfresco.maven.plugins.archiver;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Properties;
import java.util.TimeZone;
import java.util.jar.Attributes;
import java.util.jar.Manifest;

import org.apache.commons.compress.archivers.zip.ZipArchiveEntry;
import org.apache.commons.compress.archivers.zip.ZipFile;
import org.apache.maven.artifact.versioning.ComparableVersion;
import org.codehaus.plexus.archiver.ArchiverException;
import org.codehaus.plexus.archiver.zip.AbstractZipUnArchiver;

/**
 *
 * @author Axel Faust
 */
public class AmpUnArchiver extends AbstractZipUnArchiver
{

    public static final String VERSION_PROPERTIES = "WEB-INF/classes/alfresco/version.properties";

    public static final String MODULE_PROPERTIES = "module.properties";

    public static final String FILE_MAPPING_PROPERTIES = "file-mapping.properties";

    public static final String MANIFEST_FILE = "META-INF/MANIFEST.MF";

    public static final String MANIFEST_SPECIFICATION_TITLE = "Specification-Title";

    public static final String MANIFEST_SPECIFICATION_VERSION = "Specification-Version";

    public static final String MANIFEST_IMPLEMENTATION_TITLE = "Implementation-Title";

    public static final String MANIFEST_SHARE = "Alfresco Share";

    public static final String MANIFEST_COMMUNITY = "Community";

    protected static final String REGEX_NUMBER_OR_DOT = "[0-9\\.]*";

    private static final String NATIVE_ENCODING = "native-encoding";

    /**
     *
     * @author Axel Faust
     */
    private static interface FileReader<T>
    {

        T readFile(InputStream is) throws IOException;
    }

    private static final FileReader<Properties> PROPERTIES_READER = new FileReader<Properties>()
    {

        /**
         *
         * {@inheritDoc}
         */
        @Override
        public Properties readFile(final InputStream is) throws IOException
        {
            final Properties p = new Properties();
            p.load(is);
            return p;
        }
    };

    private static final FileReader<Manifest> MANIFEST_READER = new FileReader<Manifest>()
    {

        /**
         *
         * {@inheritDoc}
         */
        @Override
        public Manifest readFile(final InputStream is) throws IOException
        {
            final Manifest m = new Manifest(is);
            return m;
        }
    };

    // encoding is not exposed via a getter in base class so need to duplicate it
    private String encoding = StandardCharsets.UTF_8.name();

    private final ThreadLocal<Properties> fileMappingProperties = new ThreadLocal<>();

    private final ThreadLocal<ModuleDetails> moduleDetails = new ThreadLocal<>();

    public AmpUnArchiver()
    {
        super();
    }

    public AmpUnArchiver(final File sourceFile)
    {
        super(sourceFile);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setEncoding(String encoding)
    {
        if (NATIVE_ENCODING.equals(encoding))
        {
            encoding = null;
        }
        this.encoding = encoding;
        super.setEncoding(encoding);
    }

    /**
     * @return the encoding
     */
    public String getEncoding()
    {
        return this.encoding;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void validate() throws ArchiverException
    {
        super.validate();
        this.validateAlfrescoModuleMetadata();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void validate(final String path, final File outputDirectory) throws ArchiverException
    {
        // for some reason there is an empty validation impl in base class(es)

        final File sourceFile = this.getSourceFile();

        if (sourceFile == null)
        {
            throw new ArchiverException("The source file isn't defined.");
        }

        if (sourceFile.isDirectory())
        {
            throw new ArchiverException("The source must not be a directory.");
        }

        if (!sourceFile.exists())
        {
            throw new ArchiverException("The source file " + sourceFile + " doesn't exist.");
        }

        File destDirectory = this.getDestDirectory();
        File destFile = this.getDestFile();

        if (destDirectory == null && destFile == null)
        {
            throw new ArchiverException("The destination isn't defined.");
        }

        if (destDirectory != null && destFile != null)
        {
            throw new ArchiverException("You must choose between a destination directory and a destination file.");
        }

        if (destDirectory != null && !destDirectory.isDirectory())
        {
            this.setDestFile(destDirectory);
            this.setDestDirectory(null);
            destFile = destDirectory;
            destDirectory = null;
        }

        if (destFile != null && destFile.isDirectory())
        {
            this.setDestDirectory(destFile);
            this.setDestFile(null);
            destDirectory = destFile;
            destFile = null;
        }

        this.validateAlfrescoModuleMetadata();
    }

    protected void validateAlfrescoModuleMetadata() throws ArchiverException
    {
        final File sourceFile = this.getSourceFile();

        final File destDirectory = this.getDestDirectory();
        final File destFile = this.getDestFile();

        final Properties moduleProperties = this.loadMetaFile(sourceFile, MODULE_PROPERTIES, PROPERTIES_READER);
        if (moduleProperties == null)
        {
            throw new ArchiverException(sourceFile.getAbsolutePath() + " does not contain a module.properties file");
        }
        final ModuleDetails md = new ModuleDetailsImpl(moduleProperties);

        final File destContext = destDirectory != null && destDirectory.isDirectory() ? destDirectory
                : (destFile != null && destFile.exists() ? destFile : null);
        if (destContext != null)
        {
            final Properties versionProperties = this.loadMetaFile(destContext, VERSION_PROPERTIES, PROPERTIES_READER);
            final Manifest manifest = this.loadMetaFile(destContext, MANIFEST_FILE, MANIFEST_READER);

            if (versionProperties != null)
            {
                this.validateAlfrescoModuleAgainstVersionProperties(md, versionProperties);
            }
            else if (manifest != null)
            {
                this.validateAlfrescoModuleAgainstManifest(md, manifest);
            }
            else
            {
                this.getLogger().debug(
                        "Module cannot be validated before unpacking as neither version.properties nor manifest can be found in context "
                                + destContext);
            }

            final List<ModuleDependency> dependencies = md.getDependencies();
            if (!dependencies.isEmpty())
            {
                // TODO module dependencies check
            }
        }
        // else: nothing to validate - unpack into "nothing"
    }

    protected void validateAlfrescoModuleAgainstVersionProperties(final ModuleDetails md, final Properties versionProperties)
    {
        this.getLogger().debug("Validating " + md + " against version properties " + versionProperties);

        final StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(versionProperties.getProperty("version.major"));
        stringBuilder.append(".");
        stringBuilder.append(versionProperties.getProperty("version.minor"));
        stringBuilder.append(".");
        stringBuilder.append(versionProperties.getProperty("version.revision"));
        final String repoVersionStr = stringBuilder.toString();

        final ComparableVersion repoVersion = new ComparableVersion(repoVersionStr);
        this.validateAlfrescoVersion(md, repoVersion);

        final String edition = versionProperties.getProperty("version.edition");
        this.validateAlfrescoEdition(md, edition, false);
    }

    protected void validateAlfrescoModuleAgainstManifest(final ModuleDetails md, final Manifest manifest)
    {
        this.getLogger().debug("Validating " + md + " against manifest " + manifest);

        final Attributes mainAttributes = manifest.getMainAttributes();
        final String manifestVersionStr = mainAttributes.getValue(MANIFEST_SPECIFICATION_VERSION);
        final String edition = mainAttributes.getValue(MANIFEST_IMPLEMENTATION_TITLE);

        if (manifestVersionStr != null && manifestVersionStr.length() > 0)
        {
            if (manifestVersionStr.matches(REGEX_NUMBER_OR_DOT))
            {
                final ComparableVersion manifestVersion = new ComparableVersion(manifestVersionStr);
                this.validateAlfrescoVersion(md, manifestVersion);
            }
            else if (edition != null && edition.length() > 0 && edition.endsWith(MANIFEST_COMMUNITY))
            {
                this.getLogger()
                        .warn("Community edition web application detected, the version number is non-numeric so we will not validate it.");
            }
            else
            {
                throw new ArchiverException("Invalid version number specified: " + manifestVersionStr);
            }
        }

        if (edition != null && edition.length() > 0)
        {
            this.validateAlfrescoEdition(md, edition, true);
        }
        else
        {
            this.getLogger().warn(
                    "No edition information detected in war, edition validation is disabled, continuing anyway. Is this war prior to 3.4.11, 4.1.1 and Community 4.2 ?");
        }
    }

    protected void validateAlfrescoVersion(final ModuleDetails md, final ComparableVersion alfrescoVersion)
    {
        if (alfrescoVersion.compareTo(md.getRepoVersionMin()) < 0)
        {
            throw new ArchiverException(
                    "The module (" + md.getTitle() + ") must be installed on a web application version equal to or greater than "
                            + md.getRepoVersionMin() + ". This web application is version: " + alfrescoVersion + ".");
        }

        if (alfrescoVersion.compareTo(md.getRepoVersionMax()) > 0)
        {
            throw new ArchiverException("The module (" + md.getTitle() + ") cannot be installed on a web application version greater than "
                    + md.getRepoVersionMax() + ". This web application is version: " + alfrescoVersion + ".");
        }
    }

    protected void validateAlfrescoEdition(final ModuleDetails md, final String edition, final boolean endsWithCheck)
    {
        final List<String> supportedEditions = md.getEditions();
        if (!supportedEditions.isEmpty())
        {
            final String editionL = edition.toLowerCase(Locale.ENGLISH);
            boolean editionIsAllowed = false;
            for (final String allowedEdition : supportedEditions)
            {
                final String allowedEditionL = allowedEdition.toLowerCase(Locale.ENGLISH);
                editionIsAllowed = editionIsAllowed
                        || (endsWithCheck ? editionL.endsWith(allowedEditionL) : allowedEditionL.equals(editionL));
            }

            if (!editionIsAllowed)
            {
                throw new ArchiverException(
                        "The module (" + md.getTitle() + ") can only be installed in one of the following editions" + supportedEditions);
            }
        }
    }

    /**
     *
     * {@inheritDoc}
     */
    @Override
    protected void execute() throws ArchiverException
    {
        final File sourceFile = this.getSourceFile();

        final File destDirectory = this.getDestDirectory();
        final File destFile = this.getDestFile();

        if (destDirectory != null)
        {
            this.getLogger().debug("Unpacking " + sourceFile + " into directory " + destDirectory);

            final Properties moduleProperties = this.loadMetaFile(sourceFile, MODULE_PROPERTIES, PROPERTIES_READER);
            final ModuleDetails md = new ModuleDetailsImpl(moduleProperties);
            final Properties fileMappingProperties = this.getOrCreateDefaultFileMappings(sourceFile);

            this.fileMappingProperties.set(fileMappingProperties);
            this.moduleDetails.set(md);
            try
            {
                // regular unpack
                super.execute();
            }
            finally
            {
                this.moduleDetails.remove();
                this.fileMappingProperties.remove();
            }
        }
        else if (destFile != null)
        {
            // unpack INTO another (existing?) file - might be WAR install
            throw new ArchiverException("Unpacking into a file is not supported yet");
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void extractFile(final File srcF, final File dir, final InputStream compressedInputStream, final String entryName,
            final Date entryDate, final boolean isDirectory, final Integer mode, final String symlinkDestination)
            throws IOException, ArchiverException
    {
        final String effectiveEntryName = this.mapModuleEntryName(entryName);

        if (effectiveEntryName != null && !effectiveEntryName.isEmpty())
        {
            this.getLogger().debug("Extracting " + effectiveEntryName);

            super.extractFile(srcF, dir, compressedInputStream, effectiveEntryName, entryDate, isDirectory, mode, symlinkDestination);

            if (entryName.equals(MODULE_PROPERTIES))
            {
                this.getLogger().debug("Appending installation details to unpacked module.properties");

                final DateFormat df = new SimpleDateFormat("YYYY-MM-dd'T'hh:mm:ss.SSSXXX", Locale.ENGLISH);
                df.setTimeZone(TimeZone.getTimeZone("UTC"));
                final String nowIso = df.format(new Date());

                final StringBuilder suffix = new StringBuilder(2 * 48);
                suffix.append(System.lineSeparator());
                suffix.append(ModuleDetails.PROP_INSTALL_STATE).append("=").append(ModuleInstallState.INSTALLED.name());
                suffix.append(System.lineSeparator());
                suffix.append(ModuleDetails.PROP_INSTALL_DATE).append("=").append(nowIso);
                suffix.append(System.lineSeparator());

                final byte[] bytes = suffix.toString().getBytes(StandardCharsets.UTF_8);
                Files.write(dir.toPath().resolve(effectiveEntryName), bytes, StandardOpenOption.APPEND);
            }
        }
        else
        {
            this.getLogger().debug("Entry " + entryName + " will not be unpacked as it is not covered by any file mapping");
        }
    }

    protected String mapModuleEntryName(final String entryName)
    {
        String effectiveEntryName = null;
        String prefix = entryName;

        while (prefix.length() != 0)
        {
            final int indexOfLastPathSeparator = prefix.lastIndexOf("/");
            prefix = indexOfLastPathSeparator > 0 ? prefix.substring(0, indexOfLastPathSeparator) : "";

            final String mapping = this.fileMappingProperties.get().getProperty("/" + prefix);
            if (mapping != null)
            {
                effectiveEntryName = mapping.substring(1, mapping.length()) + entryName.substring(prefix.length(), entryName.length());
                this.getLogger().debug(
                        "Mapped entry " + entryName + " to " + effectiveEntryName + " via configured mapping /" + prefix + "=" + mapping);
                break;
            }
        }

        if (effectiveEntryName == null && entryName.equals(MODULE_PROPERTIES))
        {
            effectiveEntryName = "WEB-INF/classes/alfresco/module/" + this.moduleDetails.get().getId() + "/" + MODULE_PROPERTIES;
        }
        if (effectiveEntryName != null && effectiveEntryName.startsWith("/"))
        {
            effectiveEntryName = effectiveEntryName.substring(1);
        }

        this.getLogger().debug("Using effective entry name " + effectiveEntryName + " for base entry name " + entryName);

        return effectiveEntryName;
    }

    protected Properties getOrCreateDefaultFileMappings(final File sourceFile)
    {
        Properties fileMappingProperties = this.loadMetaFile(sourceFile, FILE_MAPPING_PROPERTIES, PROPERTIES_READER);
        if (fileMappingProperties == null)
        {
            fileMappingProperties = new Properties();
            fileMappingProperties.setProperty("include.default", "true");
        }

        if (Boolean.parseBoolean(fileMappingProperties.getProperty("include.default", "false")))
        {
            this.getLogger().debug("Expanding include.default=true in file mappings");
            fileMappingProperties.put("/config", "/WEB-INF/classes");
            fileMappingProperties.put("/lib", "/WEB-INF/lib");
            fileMappingProperties.put("/licenses", "/WEB-INF/licenses");
            fileMappingProperties.put("/web/jsp", "/jsp");
            fileMappingProperties.put("/web/css", "/css");
            fileMappingProperties.put("/web/images", "/images");
            fileMappingProperties.put("/web/scripts", "/scripts");
            fileMappingProperties.put("/web/php", "/php");
        }
        fileMappingProperties.remove("include.default");
        return fileMappingProperties;
    }

    protected <T> T loadMetaFile(final File context, final String relativePath, final FileReader<T> reader) throws ArchiverException
    {
        this.getLogger().debug("Attempting to resolve meta file " + relativePath + " in context " + context);
        T meta = null;
        try
        {
            if (context.isFile())
            {
                try (ZipFile archiveCandidate = new ZipFile(context, this.encoding, true))
                {
                    final ZipArchiveEntry zae = archiveCandidate.getEntry(relativePath);
                    if (zae != null && !zae.isDirectory())
                    {
                        try (InputStream is = archiveCandidate.getInputStream(zae))
                        {
                            meta = reader.readFile(is);
                            this.getLogger().debug("Succesfully read meta file " + relativePath + " from context " + context);
                        }
                    }
                }
            }
            else
            {
                final File propertiesFile = Paths.get(context.getPath(), relativePath).toFile();
                if (propertiesFile.exists() && propertiesFile.isFile())
                {
                    try (InputStream is = new FileInputStream(propertiesFile))
                    {
                        meta = reader.readFile(is);
                        this.getLogger().debug("Succesfully read meta file" + relativePath + " from context " + context);
                    }
                }
            }
        }
        catch (final IOException ioex)
        {
            throw new ArchiverException("Error loading file " + relativePath + " from " + context.getAbsolutePath(), ioex);
        }
        return meta;
    }
}
