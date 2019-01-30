package cpw.mods.modlauncher;

import cpw.mods.modlauncher.api.*;
import cpw.mods.modlauncher.serviceapi.*;

import java.util.*;
import java.util.function.Function;
import java.util.function.Supplier;

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

    @Override
    public Optional<ILaunchHandlerService> findLaunchHandler(final String name) {
        return launcher.findLaunchHandler(name);
    }

    @Override
    public <T> T computePropertyIfAbsent(final TypesafeMap.Key<T> key, final Function<? super TypesafeMap.Key<T>, ? extends T> valueFunction) {
        return environment.computeIfAbsent(key, valueFunction);
    }
}
