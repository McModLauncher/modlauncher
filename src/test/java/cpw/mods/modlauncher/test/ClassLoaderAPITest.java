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
import cpw.mods.modlauncher.api.IModuleLayerManager;
import org.junit.jupiter.api.Test;

import java.util.ServiceLoader;

import static org.junit.jupiter.api.Assertions.assertTrue;

class ClassLoaderAPITest extends LoggingStateTestBase {
    @Test
    void testGetResources() throws ClassNotFoundException {
        Launcher.main("--version", "1.0", "--launchTarget", "mockLaunch", "--test.mods", "A,B,C,cpw.mods.modlauncher.testjar.TestClass", "--accessToken", "SUPERSECRET!");
        ModuleLayer layer = Launcher.INSTANCE.findLayerManager()
            .flatMap(manager -> manager.getLayer(IModuleLayerManager.Layer.GAME))
            .orElseThrow();
        final Class<?> service = Thread.currentThread().getContextClassLoader().loadClass("cpw.mods.modlauncher.testjar.ITestServiceLoader");
        getClass().getModule().addUses(service);
        
        final ServiceLoader<?> load = ServiceLoader.load(layer, service);
        assertTrue(load.iterator().hasNext());
    }
}
