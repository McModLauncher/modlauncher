package cpw.mods.modlauncher;

import cpw.mods.modlauncher.api.*;
import cpw.mods.modlauncher.serviceapi.*;

import java.util.*;

/**
 * Environment implementation class
 */
public final class Environment implements IEnvironment {
    private final TypesafeMap environment;
    private final Launcher launcher;

    Environment(Launcher launcher) {
        environment = new TypesafeMap(IEnvironment.class);
        this.launcher = launcher;
    }

    @Override
    public final <T> Optional<T> getProperty(TypesafeMap.Key<T> key) {
        return environment.get(key);
    }

    @Override
    public Optional<ILaunchPluginService> findLaunchPlugin(final String name) {
        return launcher.findLaunchPlugin(name);
    }

    TypesafeMap getAll() {
        return environment;
    }
}
