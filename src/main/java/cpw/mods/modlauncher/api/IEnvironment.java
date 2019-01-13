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
     * Compute a new value for insertion into the environment, if not already present.
     *
     * @param key to insert
     * @param valueFunction the supplier of a value
     * @param <T> Type of key
     * @return The value of the key
     */
    <T> T computePropertyIfAbsent(TypesafeMap.Key<T> key, final Function<? super TypesafeMap.Key<T>, ? extends T> valueFunction);
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
        public static final Supplier<TypesafeMap.Key<String>> VERSION = buildKey("version", String.class);
        public static final Supplier<TypesafeMap.Key<Path>> GAMEDIR = buildKey("gamedir", Path.class);
        public static final Supplier<TypesafeMap.Key<Path>> ASSETSDIR = buildKey("assetsdir", Path.class);
        public static final Supplier<TypesafeMap.Key<String>> LAUNCHTARGET = buildKey("launchtarget", String.class);
    }


    static <T> Supplier<TypesafeMap.Key<T>> buildKey(String name, Class<T> clazz) {
        return new TypesafeMap.KeyBuilder<>(name, clazz, IEnvironment.class);
    }
}
