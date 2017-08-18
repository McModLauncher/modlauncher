package cpw.mods.modlauncher.api;

import java.util.*;
import java.util.function.*;

/**
 * Expose known naming domains into the system, to allow for modules to
 * lookup alternative namings.
 * notch namemappings will always be available. srg and mcp will be available in certain environments.
 */
public interface INameMappingService {
    /**
     * The name of this namemapping.
     *
     * @return a unique name for this mapping
     */
    String mappingName();

    /**
     * A version number for this namemapping.
     *
     * @return a version number for this mapping
     */
    String mappingVersion();

    /**
     * The set of mapping targets this namemapping understands.
     * Trivially, you should understand yourself.
     *
     * @return A set of other mapping targets you can translate to.
     */
    Set<String> understanding();

    /**
     * A function mapping a name to another name, for the given domain
     * and naming target.
     *
     * @param domain The naming domain
     * @param target The target from {@link #understanding} for which
     *               we wish to map the names
     * @return A function mapping names
     */
    Function<String, String> namingFunction(Domain domain, String target);

    enum Domain {CLASS, METHOD, FIELD}
}
