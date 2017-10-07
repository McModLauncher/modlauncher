package cpw.mods.modlauncher;

import java.util.*;
import java.util.function.*;
import java.util.stream.*;

/**
 * Utilities for making service loaders more stream friendly
 */
public class ServiceLoaderStreamUtils {
    static <T> void parallelForEach(ServiceLoader<T> services, Consumer<T> consumer) {
        forEach(services, consumer, true);
    }

    public static <T> void forEach(ServiceLoader<T> services, Consumer<T> consumer) {
        forEach(services, consumer, false);
    }

    private static <T> void forEach(ServiceLoader<T> services, Consumer<T> consumer, boolean parallel) {
        StreamSupport.stream(services.spliterator(), parallel).forEach(consumer);
    }

    static <T> List<T> toList(ServiceLoader<T> services) {
        return StreamSupport.stream(services.spliterator(), false).collect(Collectors.toList());
    }

    static <K,T> Map<K,T> toMap(ServiceLoader<T> services, Function<T, K> keyFunction) {
        return StreamSupport.stream(services.spliterator(), false).collect(Collectors.toMap(keyFunction, Function.identity()));
    }
}
