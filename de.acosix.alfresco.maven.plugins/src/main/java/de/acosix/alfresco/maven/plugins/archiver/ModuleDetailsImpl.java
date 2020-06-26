package de.acosix.alfresco.maven.plugins.archiver;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.StringTokenizer;
import java.util.TimeZone;

import org.apache.maven.artifact.versioning.ComparableVersion;
import org.codehaus.plexus.archiver.ArchiverException;

/**
 * Module details implementation.
 *
 * Loads details from the serialized properties file provided.
 *
 * All interfaces and classes relating to Alfresco module metadata / handling are "liberated" copies from the originals contained in the
 * alfresco-repository source project of Alfresco. The primary purpose of the "liberation" of these classes was to strip down on the
 * (transitive) dependencies required. The original classes would have required the use of alfresco-repository-XY.jar and various of its
 * (in)direct dependencies. In some aspects, functionality has been left out from being copied, e.g. anything handling with installation
 * state or overly complex value objects.
 *
 * @author Roy Wetherall (original)
 * @author Derek Hulley (original)
 * @author Axel Faust
 */
public class ModuleDetailsImpl implements ModuleDetails
{

    private final String id;

    private final List<String> aliases = new ArrayList<>();

    private final ComparableVersion version;

    private final String title;

    private final String description;

    private final List<String> editions = new ArrayList<>();

    private final ComparableVersion repoVersionMin;

    private final ComparableVersion repoVersionMax;

    private final List<ModuleDependency> dependencies = new ArrayList<>();

    private final Date installDate;

    private final ModuleInstallState installState;

    /**
     * @param id
     *            module id
     * @param versionNumber
     *            version number
     * @param title
     *            title
     * @param description
     *            description
     */
    public ModuleDetailsImpl(final String id, final ComparableVersion versionNumber, final String title, final String description)
    {
        this.id = id;
        this.version = versionNumber;
        this.title = title;
        this.description = description;

        this.repoVersionMin = VERSION_ZERO;
        this.repoVersionMax = VERSION_BIG;

        this.installDate = null;
        this.installState = ModuleInstallState.UNKNOWN;
    }

    /**
     * Creates the instance from a set of properties. All the property values are trimmed
     * and empty string values are removed from the set. In other words, zero length or
     * whitespace strings are not supported.
     *
     * @param properties
     *            the set of properties
     * @param log
     *            logger
     */
    public ModuleDetailsImpl(final Properties properties)
    {
        final Properties trimmedProperties = new Properties();
        for (final Entry<?, ?> entry : properties.entrySet())
        {
            final String key = (String) entry.getKey();
            final String value = (String) entry.getValue();
            if (value != null && value.trim().length() > 0)
            {
                trimmedProperties.setProperty(key, value.trim());
            }
        }

        final List<String> missingProperties = new ArrayList<>(1);
        this.id = trimmedProperties.getProperty(PROP_ID);
        if (this.id == null)
        {
            missingProperties.add(PROP_ID);
        }

        final String aliasesStr = trimmedProperties.getProperty(PROP_ALIASES);
        if (aliasesStr != null)
        {
            final StringTokenizer st = new StringTokenizer(aliasesStr, ",");
            while (st.hasMoreTokens())
            {
                final String alias = st.nextToken().trim();
                if (alias.length() == 0)
                {
                    continue;
                }
                this.aliases.add(alias);
            }
        }

        if (trimmedProperties.getProperty(PROP_VERSION) == null)
        {
            missingProperties.add(PROP_VERSION);
        }
        this.version = new ComparableVersion(trimmedProperties.getProperty(PROP_VERSION, "0"));

        this.title = trimmedProperties.getProperty(PROP_TITLE);
        if (this.title == null)
        {
            missingProperties.add(PROP_TITLE);
        }

        this.description = trimmedProperties.getProperty(PROP_DESCRIPTION);
        if (this.description == null)
        {
            missingProperties.add(PROP_DESCRIPTION);
        }

        if (trimmedProperties.getProperty(PROP_REPO_VERSION_MIN) != null)
        {
            this.repoVersionMin = new ComparableVersion(trimmedProperties.getProperty(PROP_REPO_VERSION_MIN));
        }
        else
        {
            this.repoVersionMin = VERSION_ZERO;
        }

        if (trimmedProperties.getProperty(PROP_REPO_VERSION_MAX) != null)
        {
            this.repoVersionMax = new ComparableVersion(trimmedProperties.getProperty(PROP_REPO_VERSION_MAX));
        }
        else
        {
            this.repoVersionMax = VERSION_BIG;
        }

        this.dependencies.addAll(extractDependencies(trimmedProperties));
        this.editions.addAll(extractEditions(trimmedProperties));

        if (trimmedProperties.getProperty(PROP_INSTALL_DATE) != null)
        {
            final String installDateStr = trimmedProperties.getProperty(PROP_INSTALL_DATE);
            try
            {
                final DateFormat df = new SimpleDateFormat("YYYY-MM-dd'T'hh:mm:ss.SSSXXX", Locale.ENGLISH);
                df.setTimeZone(TimeZone.getTimeZone("UTC"));
                this.installDate = df.parse(installDateStr);
            }
            catch (final Throwable e)
            {
                throw new ArchiverException("Unable to parse install date: " + PROP_INSTALL_DATE + ", " + installDateStr, e);
            }
        }
        else
        {
            this.installDate = null;
        }

        if (trimmedProperties.getProperty(PROP_INSTALL_STATE) != null)
        {
            final String installStateStr = trimmedProperties.getProperty(PROP_INSTALL_STATE);
            try
            {
                this.installState = ModuleInstallState.valueOf(installStateStr);
            }
            catch (final Throwable e)
            {
                throw new ArchiverException("Unable to parse install state: " + PROP_INSTALL_STATE + ", " + installStateStr, e);
            }
        }
        else
        {
            this.installState = ModuleInstallState.UNKNOWN;
        }

        if (missingProperties.size() > 0)
        {
            throw new ArchiverException("The following module properties need to be defined: " + missingProperties);
        }

        if (this.repoVersionMax.compareTo(this.repoVersionMin) < 0)
        {
            throw new ArchiverException(
                    "The max repo version must be greater than the min repo version:\n" + "   ID:               " + this.id + "\n"
                            + "   Min repo version: " + this.repoVersionMin + "\n" + "   Max repo version: " + this.repoVersionMax);
        }

        if (this.id.matches(INVALID_ID_REGEX))
        {
            throw new ArchiverException(
                    "The module ID '" + this.id + "' is invalid.  It may consist of valid characters, numbers, '.', '_' and '-'");
        }
    }

    /**
     *
     * {@inheritDoc}
     */
    @Override
    public String getId()
    {
        return this.id;
    }

    /**
     *
     * {@inheritDoc}
     */
    @Override
    public List<String> getAliases()
    {
        return this.aliases;
    }

    /**
     *
     * {@inheritDoc}
     */
    @Override
    public ComparableVersion getVersion()
    {
        return this.version;
    }

    /**
     *
     * {@inheritDoc}
     */
    @Override
    public String getTitle()
    {
        return this.title;
    }

    /**
     *
     * {@inheritDoc}
     */
    @Override
    public String getDescription()
    {
        return this.description;
    }

    /**
     *
     * {@inheritDoc}
     */
    @Override
    public List<String> getEditions()
    {
        return new ArrayList<>(this.editions);
    }

    /**
     *
     * {@inheritDoc}
     */
    @Override
    public ComparableVersion getRepoVersionMin()
    {
        return this.repoVersionMin;
    }

    /**
     *
     * {@inheritDoc}
     */
    @Override
    public ComparableVersion getRepoVersionMax()
    {
        return this.repoVersionMax;
    }

    /**
     *
     * {@inheritDoc}
     */
    @Override
    public List<ModuleDependency> getDependencies()
    {
        return new ArrayList<>(this.dependencies);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Date getInstallDate()
    {
        return this.installDate != null ? new Date(this.installDate.getTime()) : null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ModuleInstallState getInstallState()
    {
        return this.installState;
    }

    /**
     *
     * {@inheritDoc}
     */
    @Override
    public Properties getProperties()
    {
        final Properties properties = new Properties();

        properties.setProperty(PROP_ID, this.id);
        properties.setProperty(PROP_VERSION, this.version.toString());
        properties.setProperty(PROP_TITLE, this.title);
        properties.setProperty(PROP_DESCRIPTION, this.description);

        if (this.repoVersionMin != null)
        {
            properties.setProperty(PROP_REPO_VERSION_MIN, this.repoVersionMin.toString());
        }
        if (this.repoVersionMax != null)
        {
            properties.setProperty(PROP_REPO_VERSION_MAX, this.repoVersionMax.toString());
        }

        if (this.editions != null)
        {
            properties.setProperty(PROP_EDITIONS, join(this.editions.toArray(new String[this.editions.size()]), ','));
        }

        if (this.dependencies.size() > 0)
        {
            for (final ModuleDependency dependency : this.dependencies)
            {
                final String key = PROP_DEPENDS_PREFIX + dependency.getDependencyId();
                final String value = dependency.getVersionString();
                properties.setProperty(key, value);
            }
        }

        if (this.aliases.size() > 0)
        {
            final StringBuilder sb = new StringBuilder();
            boolean first = true;
            for (final String oldId : this.aliases)
            {
                if (!first)
                {
                    sb.append(", ");
                }
                sb.append(oldId);
                first = false;
            }
            properties.setProperty(PROP_ALIASES, sb.toString());
        }
        return properties;
    }

    /**
     *
     * {@inheritDoc}
     */
    @Override
    public String toString()
    {
        return "ModuleDetails[" + this.getProperties() + "]";
    }

    private static List<String> extractEditions(final Properties trimmedProperties)
    {
        final List<String> specifiedEditions = new ArrayList<>();
        final String editions = trimmedProperties.getProperty(PROP_EDITIONS);
        if (editions != null)
        {
            final StringTokenizer st = new StringTokenizer(editions, ",");
            while (st.hasMoreTokens())
            {
                specifiedEditions.add(st.nextToken());
            }
        }
        return specifiedEditions;
    }

    private static List<ModuleDependency> extractDependencies(final Properties properties)
    {
        final int prefixLength = PROP_DEPENDS_PREFIX.length();

        final List<ModuleDependency> dependencies = new ArrayList<>(2);
        for (final Entry<?, ?> entry : properties.entrySet())
        {
            final String key = (String) entry.getKey();
            final String value = (String) entry.getValue();
            if (key.startsWith(PROP_DEPENDS_PREFIX) && key.length() > prefixLength)
            {
                final String dependencyId = key.substring(prefixLength);
                final ModuleDependency dependency = new ModuleDependencyImpl(dependencyId, value);
                dependencies.add(dependency);
            }
        }
        return dependencies;
    }

    /**
     * Grateful received from Apache Commons StringUtils class
     *
     */
    private static String join(final Object[] array, final char separator)
    {
        if (array == null)
        {
            return null;
        }
        return join(array, separator, 0, array.length);
    }

    /**
     * Grateful received from Apache Commons StringUtils class
     *
     * @param array
     *            Object[]
     * @param separator
     *            char
     * @param startIndex
     *            int
     * @param endIndex
     *            int
     * @return String
     */
    private static String join(final Object[] array, final char separator, final int startIndex, final int endIndex)
    {
        if (array == null)
        {
            return null;
        }
        int bufSize = (endIndex - startIndex);
        if (bufSize <= 0)
        {
            return "";
        }

        bufSize *= ((array[startIndex] == null ? 16 : array[startIndex].toString().length()) + 1);
        final StringBuffer buf = new StringBuffer(bufSize);

        for (int i = startIndex; i < endIndex; i++)
        {
            if (i > startIndex)
            {
                buf.append(separator);
            }
            if (array[i] != null)
            {
                buf.append(array[i]);
            }
        }
        return buf.toString();
    }
}
