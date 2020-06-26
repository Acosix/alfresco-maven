package de.acosix.alfresco.maven.plugins.archiver;

import java.util.Date;
import java.util.List;
import java.util.Properties;

import org.apache.maven.artifact.versioning.ComparableVersion;

/**
 * Module details, contains the details of an Alfresco module.
 *
 * All interfaces and classes relating to Alfresco module metadata / handling are "liberated" copies from the originals contained in the
 * alfresco-repository source project of Alfresco. The primary purpose of the "liberation" of these classes was to strip down on the
 * (transitive) dependencies required. The original classes would have required the use of alfresco-repository-XY.jar and various of its
 * (in)direct dependencies. In some aspects, functionality has been left out from being copied, e.g. anything handling with installation
 * state or overly complex value objects.
 *
 * @author Roy Wetherall (original)
 * @author Axel Faust
 */
public interface ModuleDetails
{

    ComparableVersion VERSION_ZERO = new ComparableVersion("0");

    ComparableVersion VERSION_BIG = new ComparableVersion("999");

    String PROP_ID = "module.id";

    String PROP_ALIASES = "module.aliases";

    String PROP_VERSION = "module.version";

    String PROP_TITLE = "module.title";

    String PROP_DESCRIPTION = "module.description";

    String PROP_EDITIONS = "module.editions";

    String PROP_REPO_VERSION_MIN = "module.repo.version.min";

    String PROP_REPO_VERSION_MAX = "module.repo.version.max";

    String PROP_DEPENDS_PREFIX = "module.depends.";

    String PROP_INSTALL_DATE = "module.installDate";

    String PROP_INSTALL_STATE = "module.installState";

    String INVALID_ID_REGEX = ".*[^\\w.-].*";

    /**
     * Get all defined properties.
     *
     * @return Returns the properties defined by this set of details
     */
    Properties getProperties();

    /**
     * Get the id of the module
     *
     * @return module id
     */
    String getId();

    /**
     * @return Returns a list of IDs by which this module may once have been known
     */
    List<String> getAliases();

    /**
     * Get the version number of the module
     *
     * @return module version number
     */
    ComparableVersion getVersion();

    /**
     * Get the title of the module
     *
     * @return module title
     */
    String getTitle();

    /**
     * Get the description of the module
     *
     * @return module description
     */
    String getDescription();

    /**
     * @return Returns the minimum version of the repository in which the module may be active
     */
    ComparableVersion getRepoVersionMin();

    /**
     * @return Returns the maximum version of the repository in which the module may be active
     */
    ComparableVersion getRepoVersionMax();

    /**
     * @return Returns a list of module dependencies that must be present for this module
     */
    List<ModuleDependency> getDependencies();

    /**
     * Get the modules install date
     *
     * @return module install date or <tt>null</tt> if it has not been set
     */
    Date getInstallDate();

    /**
     * Get the modules install state
     *
     * @return the modules install state
     */
    ModuleInstallState getInstallState();

    /**
     *
     * @return the editions
     */
    List<String> getEditions();
}
