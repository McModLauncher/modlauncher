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

package cpw.mods.modlauncher.test;

import cpw.mods.modlauncher.api.TypesafeMap;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Typesafe map tests
 */
class TypesafeMapTests {
    @Test
    void testTypesafeMapKey() {
        TypesafeMap context = new TypesafeMap();
        assertThrows(IllegalArgumentException.class,
                () ->
                {
                    TypesafeMap.Key.getOrCreate(context, "testkey1", String.class);
                    TypesafeMap.Key.getOrCreate(context, "testkey1", Integer.class);
                }
        );

        assertAll(
                () ->
                {
                    TypesafeMap.Key.getOrCreate(context, "testkey1", String.class);
                    TypesafeMap.Key.getOrCreate(context, "testkey1", String.class);
                },
                () ->
                {
                    TypesafeMap.Key.getOrCreate(context, "testkey1", String.class);
                    TypesafeMap.Key.getOrCreate(context, "testkey2", Integer.class);
                }
        );
    }

    @Test
    void testTypesafeMap() {
        TypesafeMap b = new TypesafeMap();
        TypesafeMap.Key<String> mykey = TypesafeMap.Key.getOrCreate(b, "testkey1", String.class);
        assertEquals(b.get(mykey), Optional.empty(), "Key not found");
        b.computeIfAbsent(mykey, s -> "Hello");
        assertEquals(b.get(mykey), Optional.of("Hello"), "Found key");
    }
}
