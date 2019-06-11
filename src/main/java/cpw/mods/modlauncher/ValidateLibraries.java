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

import java.util.AbstractMap;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

class ValidateLibraries {
    static void validate() {
        List<Map.Entry<String,String>> toCheck = Arrays.asList(
                pair("log4j", "org.apache.logging.log4j.LogManager"),
                pair("asm", "org.objectweb.asm.ClassVisitor"),
                pair("joptsimple", "joptsimple.OptionParser")
        );
        final List<Map.Entry<String, String>> brokenLibs = toCheck.stream().filter(ValidateLibraries::tryLoad).collect(Collectors.toList());
        brokenLibs.forEach(e->System.err.println("Failed to find class associated with library "+e.getKey()));
        if (!brokenLibs.isEmpty()) throw new InvalidLauncherSetupException("Missing classes, cannot continue");
    }

    private static boolean tryLoad(final Map.Entry<String, String> nameClazz) {
        try {
            Class.forName(nameClazz.getValue(), false, ClassLoader.getSystemClassLoader());
            return false;
        } catch (ClassNotFoundException e) {
            return true;
        }
    }

    private static Map.Entry<String,String> pair(String name, String clazzName) {
        return new AbstractMap.SimpleEntry<>(name, clazzName);
    }
}
