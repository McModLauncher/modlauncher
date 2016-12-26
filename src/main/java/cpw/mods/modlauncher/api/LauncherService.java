package cpw.mods.modlauncher.api;

import joptsimple.OptionSpec;
import joptsimple.OptionSpecBuilder;

import java.util.List;
import java.util.function.BiFunction;

/**
 * Users who wish to provide a mod service which plugs into this API
 * should implement this interface, and provide a {@link java.util.ServiceLoader}
 * configuration file pointing at their implementation.
 */
public interface LauncherService
{
    /**
     * The name of this mod service. It will be used throughout the system. It should be lower case,
     * the first character should be alphanumeric and it should only consist of standard alphanumeric
     * characters
     * @return the name of the mod service
     */
    String name();

    /**
     * Define command line arguments for your mod service. These will be prefixed by your {@link #name()}
     * to prevent collisions.
     *
     * @param argumentBuilder a function mapping name, description to a set of JOptSimple properties for that argument
     */
    default void arguments(BiFunction<String, String, OptionSpecBuilder> argumentBuilder) {}

    default void argumentValues(OptionResult option) {}

    /**
     * @return
     */
    List<Transformer<?>> transformers();

    interface OptionResult {
        <V> V value(OptionSpec<V> options);
        <V> List<V> values(OptionSpec<V> options);
    }
}
