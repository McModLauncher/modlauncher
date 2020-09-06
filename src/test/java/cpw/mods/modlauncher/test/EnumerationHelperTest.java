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

package cpw.mods.modlauncher.test;

import cpw.mods.modlauncher.EnumerationHelper;
import org.junit.jupiter.api.Test;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

class EnumerationHelperTest {

    @Test
    void merge() {
        final List<String> strs1 = Arrays.asList("one", "two", "three");
        Vector<String> str1 = new Vector<>(strs1);
        final List<String> strs2 = Arrays.asList("four", "five", "six");
        Vector<String> str2 = new Vector<>(strs2);
        final ArrayList<String> result = Collections.list(EnumerationHelper.merge(str1.elements(), str2.elements()));
        assertArrayEquals(Stream.concat(strs1.stream(), strs2.stream()).toArray(String[]::new), result.toArray(new String[0]));
    }

    @Test
    void fromOptional() {
        final Function<String, Enumeration<String>> function = EnumerationHelper.fromOptional(Optional::ofNullable);
        assertTrue(function.apply("result").hasMoreElements(), "has more");
        assertFalse(function.apply(null).hasMoreElements(), "has no more");
        assertEquals("result", function.apply("result").nextElement(),"returns element as first result");
        final Enumeration<String> result = function.apply("result");
        result.nextElement();
        assertFalse(result.hasMoreElements(), "has no more");
    }
}