package cpw.mods.modlauncher.api;

import joptsimple.*;
import org.checkerframework.checker.nullness.qual.*;

import java.util.*;
import java.util.function.*;

/**
 * Users who wish to provide a mod service which plugs into this API
 * should implement this interface, and provide a {@link java.util.ServiceLoader}
 * configuration file pointing at their implementation.
 */
public interface ITransformationService {
    /**
     * The name of this mod service. It will be used throughout the system. It should be lower case,
     * the first character should be alphanumeric and it should only consist of standard alphanumeric
     * characters
     *
     * @return the name of the mod service
     */
    @NonNull
    String name();

    /**
     * Define command line arguments for your mod service. These will be prefixed by your {@link #name()}
     * to prevent collisions.
     *
     * @param argumentBuilder a function mapping name, description to a set of JOptSimple properties for that argument
     */
    default void arguments(BiFunction<String, String, OptionSpecBuilder> argumentBuilder) {
    }

    default void argumentValues(OptionResult option) {
    }

    /**
     * Initialize your service. Scan for mods (but don't classload them), identify metadata that might drive
     * game functionality.
     *
     * @param environment environment - query state from here to determine viability
     */
    void initialize(IEnvironment environment);

    /**
     * Load your service. Called immediately on loading with a list of other services found.
     * Use to identify and immediately indicate incompatibilities with other services, and environment
     * configuration. This is to try and immediately abort a guaranteed bad environment.
     *
     * @param env           environment - query state from here
     * @param otherServices other services loaded with the system
     * @throws IncompatibleEnvironmentException if there is an incompatibility detected. Identify specifics in
     *                                          the exception message
     */
    void onLoad(IEnvironment env, Set<String> otherServices) throws IncompatibleEnvironmentException;

    @NonNull
    List<ITransformer<?>> transformers();

    interface OptionResult {
        @NonNull
        <V> V value(OptionSpec<V> options);

        @NonNull
        <V> List<V> values(OptionSpec<V> options);
    }

}
