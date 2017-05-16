package cpw.mods.modlauncher;

import java.util.List;
import java.util.ServiceLoader;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

/**
 * Utilities for making service loaders more stream friendly
 */
@SuppressWarnings("WeakerAccess")
public class ServiceLoaderStreamUtils
{
    static <T> void parallelForEach(ServiceLoader<T> services, Consumer<T> consumer)
    {
        forEach(services, consumer, true);
    }

    public static <T> void forEach(ServiceLoader<T> services, Consumer<T> consumer)
    {
        forEach(services, consumer, false);
    }

    private static <T> void forEach(ServiceLoader<T> services, Consumer<T> consumer, boolean parallel)
    {
        StreamSupport.stream(services.spliterator(), parallel).forEach(consumer);
    }

    static <T> List<T> toList(ServiceLoader<T> services)
    {
        return StreamSupport.stream(services.spliterator(), false).collect(Collectors.toList());
    }
}
