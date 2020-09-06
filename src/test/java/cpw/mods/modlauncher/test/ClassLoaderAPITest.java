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

import cpw.mods.modlauncher.Launcher;
import cpw.mods.modlauncher.TransformingClassLoader;
import cpw.mods.modlauncher.testjar.ITestServiceLoader;
import org.junit.jupiter.api.Test;
import org.powermock.reflect.Whitebox;

import javax.xml.ws.Service;
import java.io.File;
import java.util.List;
import java.util.ServiceLoader;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertTrue;

public class ClassLoaderAPITest {
    @Test
    void testGetResources() {
        final List<String> testJars = Stream.of(System.getProperty("java.class.path").split(File.pathSeparator)).filter(s -> s.contains("testJars")).collect(Collectors.toList());
        String testJarPath = testJars.get(0);
        Launcher.main("--version", "1.0", "--minecraftJar", testJarPath, "--launchTarget", "mockLaunch", "--test.mods", "A,B,C,cpw.mods.modlauncher.testjar.TestClass", "--accessToken", "SUPERSECRET!");
        Launcher instance = Launcher.INSTANCE;
        TransformingClassLoader tcl = Whitebox.getInternalState(instance, "classLoader");
        final ServiceLoader<ITestServiceLoader> load = ServiceLoader.load(ITestServiceLoader.class, tcl);
        assertTrue(load.iterator().hasNext());
    }
}
