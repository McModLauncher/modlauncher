package cpw.mods.modlauncher;

import cpw.mods.modlauncher.api.*;

import java.util.*;

/**
 * Environment implementation class
 */
public final class Environment implements IEnvironment {
    private final TypesafeMap environment;

    Environment() {
        environment = new TypesafeMap(IEnvironment.class);
    }

    @Override
    public final <T> Optional<T> getProperty(TypesafeMap.Key<T> key) {
        return environment.get(key);
    }

    TypesafeMap getAll() {
        return environment;
    }
}
