/*
 * ModLauncher - for launching Java programs with in-flight transformation ability.
 *
 *     Copyright (C) 2017-2019 cpw
 *
 *     This program is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU Lesser General Public License as published by
 *     the Free Software Foundation, version 3 of the License.
 *
 *     This program is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU Lesser General Public License for more details.
 *
 *     You should have received a copy of the GNU Lesser General Public License
 *     along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package cpw.mods.modlauncher;


import org.jetbrains.annotations.Nullable;
import java.util.*;
import java.util.function.*;
import java.util.stream.*;

/**
 * Utilities for making service loaders more stream friendly
 */
public class ServiceLoaderStreamUtils {
    public static <T> void parallelForEach(ServiceLoader<T> services, Consumer<T> consumer) {
        forEach(services, consumer, true);
    }

    public static <T> void forEach(ServiceLoader<T> services, Consumer<T> consumer) {
        forEach(services, consumer, false);
    }

    private static <T> void forEach(ServiceLoader<T> services, Consumer<T> consumer, boolean parallel) {
        StreamSupport.stream(services.spliterator(), parallel).forEach(consumer);
    }

    public static <T, U> Stream<U> map(ServiceLoader<T> services, Function<T, U> function) {
        return StreamSupport.stream(services.spliterator(), false).map(function);
    }

    public static <T> List<T> toList(ServiceLoader<T> services) {
        return StreamSupport.stream(services.spliterator(), false).collect(Collectors.toList());
    }

    public static <K,T> Map<K,T> toMap(ServiceLoader<T> services, Function<T, K> keyFunction) {
        return toMap(services, keyFunction, Function.identity());
    }

    public static <K,V,T> Map<K,V> toMap(ServiceLoader<T> services, Function<T, K> keyFunction, Function<T, V> valueFunction) {
        return StreamSupport.stream(services.spliterator(), false).collect(Collectors.toMap(keyFunction, valueFunction));
    }

    public static <T> ServiceLoader<T> errorHandlingServiceLoader(Class<T> clazz, Consumer<ServiceConfigurationError> errorHandler) {
        return errorHandlingServiceLoader(clazz, null, errorHandler);
    }

    public static <T> ServiceLoader<T> errorHandlingServiceLoader(Class<T> clazz, @Nullable ClassLoader cl, Consumer<ServiceConfigurationError> errorHandler) {
        final ServiceLoader<T> serviceLoader = ServiceLoader.load(clazz, cl);
        for (Iterator<T> iterator = serviceLoader.iterator(); iterator.hasNext(); ) {
            try {
                iterator.next();
            } catch (ServiceConfigurationError e) {
                errorHandler.accept(e);
            }
        }
        return serviceLoader;
    }
}
