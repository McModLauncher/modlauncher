/*
 * ModLauncher - for launching Java programs with in-flight transformation ability.
 *
 *     Copyright (C) 2017-2020 cpw
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

import java.net.URL;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class EnumerationHelper {
    public static <T> Enumeration<T> merge(Enumeration<T> first, Enumeration<T> second) {
        return new Enumeration<T>() {
            @Override
            public boolean hasMoreElements() {
                return first.hasMoreElements() || second.hasMoreElements();
            }

            @Override
            public T nextElement() {
                return first.hasMoreElements() ? first.nextElement() : second.nextElement();
            }
        };
    }

    public static <T> Function<String, Enumeration<T>> mergeFunctors(Function<String, Enumeration<T>> first, Function<String, Enumeration<T>> second) {
        return input -> merge(first.apply(input), second.apply(input));
    }

    public static <T> T firstElementOrNull(final Enumeration<T> enumeration) {
        return enumeration.hasMoreElements() ? enumeration.nextElement() : null;
    }

    public static <T> Function<String, Enumeration<T>> fromOptional(final Function<String, Optional<T>> additionalClassBytesLocator) {
        return input -> Collections.enumeration(additionalClassBytesLocator.apply(input).map(Stream::of).orElseGet(Stream::empty).collect(Collectors.toList()));
    }
}
