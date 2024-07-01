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

import cpw.mods.cl.JarModuleFinder;
import cpw.mods.jarhandling.SecureJar;
import cpw.mods.modlauncher.*;
import cpw.mods.modlauncher.api.IEnvironment;
import cpw.mods.modlauncher.api.ITransformer;
import cpw.mods.modlauncher.api.TypesafeMap;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;
import org.powermock.reflect.Whitebox;

import java.lang.module.Configuration;
import java.lang.module.ModuleFinder;
import java.lang.reflect.Constructor;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Test class loader
 */
class TransformingClassLoaderTests {
    private static final String TARGET_CLASS = "cpw.mods.modlauncher.testjar.TestClass";

    @Test
    void testClassLoader() throws Exception {
        MockTransformerService mockTransformerService = new MockTransformerService() {
            @NotNull
            @Override
            public List<? extends ITransformer<?>> transformers() {
                return Stream.of(new ClassNodeTransformer(List.of(TARGET_CLASS))).collect(Collectors.toList());
            }
        };

        TransformStore transformStore = new TransformStore();
        ModuleLayerHandler layerHandler = new ModuleLayerHandler();
        LaunchPluginHandler lph = new LaunchPluginHandler(layerHandler);
        TransformationServiceDecorator sd = Whitebox.invokeConstructor(TransformationServiceDecorator.class, mockTransformerService);
        sd.gatherTransformers(transformStore);
        
        Environment environment = new Environment(
                s -> Optional.empty(),
                s -> Optional.empty(),
                layerHandler
        );
        new TypesafeMap(IEnvironment.class);
        Configuration configuration = createTestJarsConfiguration();
        TransformingClassLoader tcl = new TransformingClassLoader(transformStore, lph, environment, configuration, List.of(ModuleLayer.boot()));
        ModuleLayer.boot().defineModules(configuration, s -> tcl);
        
        final Class<?> aClass = Class.forName(TARGET_CLASS, true, tcl);
        assertEquals(Whitebox.getField(aClass, "testfield").getType(), String.class);
        assertEquals(Whitebox.getField(aClass, "testfield").get(null), "CHEESE!");

        final Class<?> newClass = tcl.loadClass(TARGET_CLASS);
        assertEquals(aClass, newClass, "Class instance is the same from Class.forName and tcl.loadClass");
    }
    
    private Configuration createTestJarsConfiguration() {
        SecureJar testJars = SecureJar.from(Path.of(System.getProperty("testJars.location")));
        JarModuleFinder finder = JarModuleFinder.of(testJars);
        return ModuleLayer.boot().configuration().resolveAndBind(finder, ModuleFinder.ofSystem(), Set.of("cpw.mods.modlauncher.testjars"));
    }
}
