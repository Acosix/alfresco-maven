package de.acosix.alfresco.maven.plugins.archiver;

import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;

import org.apache.maven.artifact.versioning.ComparableVersion;
import org.codehaus.plexus.archiver.ArchiverException;

/**
 * Implementation for the metadata value object of a single module dependency.
 *
 * All interfaces and classes relating to Alfresco module metadata / handling are "liberated" copies from the originals contained in the
 * alfresco-repository source project of Alfresco. The primary purpose of the "liberation" of these classes was to strip down on the
 * (transitive) dependencies required. The original classes would have required the use of alfresco-repository-XY.jar and various of its
 * (in)direct dependencies. In some aspects, functionality has been left out from being copied, e.g. anything handling with installation
 * state or overly complex value objects.
 *
 * @author Derek Hulley (original)
 * @author Axel Faust
 */
public class ModuleDependencyImpl implements ModuleDependency
{

    /**
     *
     * @author Axel Faust
     */
    protected static class VersionRange
    {

        private final ComparableVersion lowerBound;

        private final ComparableVersion upperBound;

        protected VersionRange(final ComparableVersion lowerBound, final ComparableVersion upperBound)
        {
            this.lowerBound = lowerBound;
            this.upperBound = upperBound;
        }

        /**
         * @return the lowerBound
         */
        public ComparableVersion getLowerBound()
        {
            return this.lowerBound;
        }

        /**
         * @return the upperBound
         */
        public ComparableVersion getUpperBound()
        {
            return this.upperBound;
        }

        public boolean matches(final ComparableVersion checkVersion)
        {
            boolean matches = true;

            matches = matches && (this.lowerBound == null || this.lowerBound.compareTo(checkVersion) <= 0);
            matches = matches && (this.upperBound == null || this.upperBound.compareTo(checkVersion) >= 0);

            return matches;
        }
    }

    private final String dependencyId;

    private final String versionString;

    private final List<VersionRange> versionRanges;

    public ModuleDependencyImpl(final String dependencyId, final String versionString)
    {
        this.dependencyId = dependencyId;
        this.versionString = versionString;

        try
        {
            this.versionRanges = buildVersionRanges(versionString);
        }
        catch (final Throwable e)
        {
            throw new ArchiverException("Unable to interpret the module version ranges: " + versionString, e);
        }
    }

    /**
     *
     * {@inheritDoc}
     */
    @Override
    public String getDependencyId()
    {
        return this.dependencyId;
    }

    /**
     *
     * {@inheritDoc}
     */
    @Override
    public String getVersionString()
    {
        return this.versionString;
    }

    /**
     *
     * {@inheritDoc}
     */
    @Override
    public boolean isValidDependency(final ModuleDetails moduleDetails)
    {
        boolean isValid = true;

        isValid = isValid && moduleDetails != null;
        isValid = isValid && moduleDetails.getId().equals(this.dependencyId);

        if (isValid)
        {
            final ComparableVersion checkVersion = moduleDetails.getVersion();

            boolean versionMatches = false;
            for (final VersionRange versionRange : this.versionRanges)
            {
                versionMatches = versionMatches || versionRange.matches(checkVersion);
            }
            isValid = isValid && versionMatches;
        }
        return isValid;
    }

    private static List<VersionRange> buildVersionRanges(final String versionStr)
    {
        final List<VersionRange> versionRanges = new ArrayList<>(1);
        final StringTokenizer rangesTokenizer = new StringTokenizer(versionStr, ",");
        while (rangesTokenizer.hasMoreTokens())
        {
            String range = rangesTokenizer.nextToken().trim();
            if (range.equals("*"))
            {
                range = "*-*";
            }
            if (range.startsWith("-"))
            {
                range = "*" + range;
            }
            if (range.endsWith("-"))
            {
                range = range + "*";
            }

            final StringTokenizer rangeTokenizer = new StringTokenizer(range, "-", false);
            ComparableVersion versionLower = null;
            ComparableVersion versionUpper = null;
            while (rangeTokenizer.hasMoreTokens())
            {
                String version = rangeTokenizer.nextToken();
                version = version.trim();
                if (versionLower == null)
                {
                    if (version.equals("*"))
                    {
                        versionLower = ModuleDetails.VERSION_ZERO;
                    }
                    else
                    {
                        versionLower = new ComparableVersion(version);
                    }
                }
                else if (versionUpper == null)
                {
                    if (version.equals("*"))
                    {
                        versionUpper = ModuleDetails.VERSION_BIG;
                    }
                    else
                    {
                        versionUpper = new ComparableVersion(version);
                    }
                }
            }
            // Check
            if (versionUpper == null && versionLower == null)
            {
                throw new ArchiverException("Valid dependency version ranges are: \n" + "   LOW  - HIGH \n" + "   *    - HIGH \n"
                        + "   LOW  - *    \n" + "   *       ");
            }
            else if (versionUpper == null && versionLower != null)
            {
                versionUpper = versionLower;
            }
            else if (versionLower == null && versionUpper != null)
            {
                versionLower = versionUpper;
            }

            versionRanges.add(new VersionRange(versionLower, versionUpper));
        }
        return versionRanges;
    }
}
