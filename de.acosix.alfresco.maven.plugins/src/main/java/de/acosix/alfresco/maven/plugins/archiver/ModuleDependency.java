package de.acosix.alfresco.maven.plugins.archiver;

/**
 * An encapsulated module dependency. Since module dependencies may be range based and even
 * unbounded, it is not possible to describe a dependency using a list of module version numbers.
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
public interface ModuleDependency
{

    /**
     * Get the ID of the module that this dependency describes. The dependency
     * may be upon specific versions or a range of versions. Nevertheless, the
     * module given by the returned ID will be required in one version or another.
     *
     * @return Returns the ID of the module that this depends on
     */
    String getDependencyId();

    /**
     * @return Returns a string representation of the versions supported
     */
    String getVersionString();

    /**
     * Check if a module satisfies the dependency requirements.
     *
     * @param moduleDetails
     *            the module details of the dependency. This must be
     *            the details of the module with the correct
     *            {@link #getDependencyId() ID}. This may be <tt>null</tt>
     *            in which case <tt>false</tt> will always be returned.
     * @return Returns true if the module satisfies the dependency
     *         requirements.
     */
    boolean isValidDependency(ModuleDetails moduleDetails);
}
