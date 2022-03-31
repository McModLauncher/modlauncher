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

import cpw.mods.modlauncher.ArgumentHandler;
import cpw.mods.modlauncher.Launcher;
import cpw.mods.modlauncher.TransformationServiceDecorator;
import cpw.mods.modlauncher.api.IEnvironment;
import cpw.mods.modlauncher.api.IModuleLayerManager;
import cpw.mods.modlauncher.api.ITransformationService;
import joptsimple.OptionSet;
import joptsimple.OptionSpec;
import org.junit.jupiter.api.Test;
import org.powermock.reflect.Whitebox;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Field;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test overall launcher
 */
class LauncherTests {
    @Test
    void testLauncher() throws Exception {
        Launcher.main("--version", "1.0", "--launchTarget", "mockLaunch", "--test.mods", "A,B,C,cpw.mods.modlauncher.testjar.TestClass,cheese.Puffs", "--accessToken", "SUPERSECRET!");
        Launcher instance = Launcher.INSTANCE;
        final Map<String, TransformationServiceDecorator> services = Whitebox.getInternalState(Whitebox.getInternalState(instance, "transformationServicesHandler"), "serviceLookup");
        final List<ITransformationService> launcherServices = services.values().stream()
            .map(dec -> Whitebox.<ITransformationService>getInternalState(dec, "service"))
            .toList();
        assertAll("services are present and correct",
                () -> assertEquals(1, launcherServices.size(), "Found 1 service"),
                () -> assertEquals(MockTransformerService.class, launcherServices.get(0).getClass(), "Found Test Launcher Service")
        );

        final ArgumentHandler argumentHandler = Whitebox.getInternalState(instance, "argumentHandler");
        final OptionSet options = Whitebox.getInternalState(argumentHandler, "optionSet");
        Map<String, OptionSpec<?>> optionsMap = options.specs().stream().collect(Collectors.toMap(s -> String.join(",", s.options()), s -> s, (u, u2) -> u));

        assertAll("options are correctly setup",
                () -> assertTrue(optionsMap.containsKey("version"), "Version field is correct"),
                () -> assertTrue(optionsMap.containsKey("test.mods"), "Test service option is correct")
        );

        final MockTransformerService mockTransformerService = (MockTransformerService) launcherServices.get(0);
        assertAll("test launcher service is correctly configured",
                () -> assertIterableEquals(Arrays.asList("A", "B", "C", "cpw.mods.modlauncher.testjar.TestClass", "cheese.Puffs"), Whitebox.getInternalState(mockTransformerService, "modList"), "modlist is configured"),
                () -> assertEquals("INITIALIZED", Whitebox.getInternalState(mockTransformerService, "state"), "Initialized was called")
        );

        assertAll(
                () -> assertNotNull(instance.environment().getProperty(IEnvironment.Keys.VERSION.get()))
        );

        try {
            final Stream<Field> transformedFields = Stream.of(Class.forName("cpw.mods.modlauncher.testjar.TestClass", true, Whitebox.getInternalState(Launcher.INSTANCE, "classLoader")).getDeclaredFields());
            assertTrue(transformedFields.anyMatch(f -> f.getName().equals("testfield")), "Found transformed field");
            final Module testJarsModule = Launcher.INSTANCE.findLayerManager()
                    .flatMap(m -> m.getLayer(IModuleLayerManager.Layer.PLUGIN))
                    .flatMap(l -> l.findModule("cpw.mods.modlauncher.testjars"))
                    .orElseThrow();
            final Stream<Field> untransformedFields = Stream.of(Class.forName(testJarsModule, "cpw.mods.modlauncher.testjar.TestClass").getDeclaredFields());
            assertTrue(untransformedFields.noneMatch(f -> f.getName().equals("testfield")), "Didn't find transformed field");
            
            final Class<?> generatedClass = Class.forName("cheese.Puffs", true, Whitebox.getInternalState(Launcher.INSTANCE, "classLoader"));
            assertEquals("cheese.Puffs", generatedClass.getName());
            assertTrue(Stream.of(generatedClass.getDeclaredFields()).anyMatch(f -> f.getName().equals("testfield")), "Found generated field");

            Class<?> resClass = Class.forName("cpw.mods.modlauncher.testjar.ResourceLoadingClass", true, Whitebox.getInternalState(Launcher.INSTANCE, "classLoader"));
            assertFindResource(resClass);
        } catch (ClassNotFoundException e) {
            fail("Can't load class");
        }
    }

    private void assertFindResource(Class<?> loaded) throws Exception {
        Object instance = loaded.getDeclaredConstructor().newInstance();
        URL resource = (URL) Whitebox.getField(loaded, "resource").get(instance);
        assertNotNull(resource, "Resource not found");
        // assert that we can find something in the resource, so we know it loaded properly
        try (InputStream in = resource.openStream();
             Scanner scanner = new Scanner(new InputStreamReader(in, StandardCharsets.UTF_8))) {
            assertTrue(scanner.nextLine().contains("Loaded successfully!"), "Resource has incorrect content");
        }
    }
}
