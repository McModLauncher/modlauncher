package cpw.mods.modlauncher.api;

import java.io.*;
import java.util.*;
import java.util.function.*;

/**
 * System environment. Global properties relevant to the current environment.
 */
public interface IEnvironment {
    <T> Optional<T> getProperty(TypesafeMap.Key<T> key);

    final class Keys {
        public static final Supplier<TypesafeMap.Key<String>> VERSION = new TypesafeMap.KeyBuilder<>("version", String.class, IEnvironment.class);
        public static final Supplier<TypesafeMap.Key<File>> GAMEDIR = new TypesafeMap.KeyBuilder<>("gamedir", File.class, IEnvironment.class);
        public static final Supplier<TypesafeMap.Key<File>> ASSETSDIR = new TypesafeMap.KeyBuilder<>("assetsdir", File.class, IEnvironment.class);
    }
}
