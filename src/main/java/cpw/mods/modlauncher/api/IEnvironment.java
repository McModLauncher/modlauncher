package cpw.mods.modlauncher.api;

import cpw.mods.modlauncher.serviceapi.*;

import java.nio.file.*;
import java.util.*;
import java.util.function.*;

/**
 * System environment. Global properties relevant to the current environment and lookups to find global artifacts
 * in the environment.
 */
public interface IEnvironment {
    /**
     * Get a property from the Environment
     * @param key to find
     * @param <T> Type of key
     * @return the value
     */
    <T> Optional<T> getProperty(TypesafeMap.Key<T> key);

    /**
     * Find the named {@link ILaunchPluginService}
     *
     * @param name name to lookup
     * @return the launch plugin
     */
    Optional<ILaunchPluginService> findLaunchPlugin(String name);

    /**
     * Find the named {@link ILaunchHandlerService}
     *
     * @param name name to lookup
     * @return the launch handler
     */
    Optional<ILaunchHandlerService> findLaunchHandler(String name);

    final class Keys {
        public static final Supplier<TypesafeMap.Key<String>> VERSION = new TypesafeMap.KeyBuilder<>("version", String.class, IEnvironment.class);
        public static final Supplier<TypesafeMap.Key<Path>> GAMEDIR = new TypesafeMap.KeyBuilder<>("gamedir", Path.class, IEnvironment.class);
        public static final Supplier<TypesafeMap.Key<Path>> ASSETSDIR = new TypesafeMap.KeyBuilder<>("assetsdir", Path.class, IEnvironment.class);
        public static final Supplier<TypesafeMap.Key<String>> LAUNCHTARGET = new TypesafeMap.KeyBuilder<>("launchtarget", String.class, IEnvironment.class);
    }
}
