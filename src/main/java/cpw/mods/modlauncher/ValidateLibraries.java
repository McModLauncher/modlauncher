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
        if (System.getProperty("java.vendor").contains("OpenJ9")) {
            System.err.println("You are attempting to run with an unsupported Java Virtual Machine : "+System.getProperty("java.vendor"));
            System.err.println("Please visit https://adoptopenjdk.net and install the HotSpot variant.");
            System.err.println("OpenJ9 is incompatible with several of the transformation behaviours that we rely on to work.");
            throw new IllegalStateException("Open J9 is not supported");
        }

        List<Map.Entry<String,String>> toCheck = Arrays.asList(
                pair("log4j", "org.apache.logging.log4j.LogManager"),
                pair("asm", "org.objectweb.asm.ClassVisitor"),
                pair("joptsimple", "joptsimple.OptionParser")
        );
        var moduleList = ValidateLibraries.class.getModule().getLayer().modules().stream().map(Module::getName).toList();
    }

    private static Map.Entry<String,String> pair(String name, String clazzName) {
        return new AbstractMap.SimpleEntry<>(name, clazzName);
    }
}
