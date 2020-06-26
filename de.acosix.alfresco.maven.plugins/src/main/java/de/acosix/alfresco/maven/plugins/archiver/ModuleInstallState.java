package de.acosix.alfresco.maven.plugins.archiver;

/**
 * Enum used to indicate the install state of a module.
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
public enum ModuleInstallState
{
    /** The state of the module is unknown */
    UNKNOWN,
    /** The module is installed */
    INSTALLED,
    /** The module is disabled */
    DISABLED,
    /** The module has been uninstalled */
    UNINSTALLED;
}
