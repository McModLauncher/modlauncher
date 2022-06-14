/*
 * ModLauncher - for launching Java programs with in-flight transformation ability.
 *
 *     Copyright (C) 2017-2021 cpw
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

package cpw.mods.modlauncher.util;

import cpw.mods.niofs.union.UnionFileSystem;

import java.nio.file.Path;
import java.util.Objects;
import java.util.ServiceConfigurationError;
import java.util.ServiceLoader;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.stream.Stream;

public final class ServiceLoaderUtils {
    public static <T> Stream<T> streamServiceLoader(Supplier<ServiceLoader<T>> slSupplier, Consumer<ServiceConfigurationError> errorConsumer) {
        return streamWithErrorHandling(slSupplier.get(), errorConsumer);
    }

    public static <T> Stream<T> streamWithErrorHandling(ServiceLoader<T> sl, Consumer<ServiceConfigurationError> errorConsumer) {
        return sl.stream().map(p->{
            try {
                return p.get();
            } catch (ServiceConfigurationError sce) {
                errorConsumer.accept(sce);
                return null;
            }
        }).filter(Objects::nonNull);
    }

    public static String fileNameFor(Class<?> clazz) {
        return clazz.getModule().getLayer().configuration()
                .findModule(clazz.getModule().getName())
                .flatMap(rm->rm.reference().location())
                .map(Path::of)
                .map(p -> p.getFileSystem() instanceof UnionFileSystem ufs ? ufs.getPrimaryPath() : p)
                .map(p -> p.getFileName().toString())
                .orElse("MISSING FILE");
    }
}
