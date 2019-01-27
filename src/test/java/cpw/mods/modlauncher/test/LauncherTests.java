package cpw.mods.modlauncher.test;

import cpw.mods.modlauncher.*;
import cpw.mods.modlauncher.api.*;
import joptsimple.*;
import org.junit.jupiter.api.*;
import org.powermock.reflect.*;

import java.io.*;
import java.lang.reflect.*;
import java.util.*;
import java.util.stream.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test overall launcher
 */
class LauncherTests {
    @Test
    void testLauncher() throws IllegalAccessException {
        final List<String> testJars = Stream.of(System.getProperty("java.class.path").split(File.pathSeparator)).filter(s -> s.contains("testJars")).collect(Collectors.toList());
        String testJarPath = testJars.get(0);
        Launcher.main("--version", "1.0", "--minecraftJar", testJarPath, "--launchTarget", "mockLaunch", "--test.mods", "A,B,C,cpw.mods.modlauncher.testjar.TestClass", "--accessToken", "SUPERSECRET!");
        Launcher instance = Launcher.INSTANCE;
        final ServiceLoader<ITransformationService> services = Whitebox.getInternalState(Whitebox.getInternalState(instance, "transformationServicesHandler"), "transformationServices");
        final List<ITransformationService> launcherServices = StreamSupport.stream(services.spliterator(), false).collect(Collectors.toList());
        assertAll("services are present and correct",
                () -> assertEquals(1, launcherServices.size(), "Found 1 service"),
                () -> assertEquals(MockTransformerService.class, launcherServices.get(0).getClass(), "Found Test Launcher Service")
        );

        final ArgumentHandler argumentHandler = Whitebox.getInternalState(instance, "argumentHandler");
        final OptionSet options = Whitebox.getInternalState(argumentHandler, "optionSet");
        Map<String, OptionSpec<?>> optionsMap = options.specs().stream().collect(Collectors.toMap(s -> s.options().stream().collect(Collectors.joining(",")), s -> s, (u, u2) -> u));

        assertAll("options are correctly setup",
                () -> assertTrue(optionsMap.containsKey("version"), "Version field is correct"),
                () -> assertTrue(optionsMap.containsKey("test.mods"), "Test service option is correct")
        );

        final MockTransformerService mockTransformerService = (MockTransformerService) launcherServices.get(0);
        assertAll("test launcher service is correctly configured",
                () -> assertIterableEquals(Arrays.asList("A", "B", "C", "cpw.mods.modlauncher.testjar.TestClass"), Whitebox.getInternalState(mockTransformerService, "modList"), "modlist is configured"),
                () -> assertEquals(Whitebox.getInternalState(mockTransformerService, "state"), "INITIALIZED", "Initialized was called")
        );

        assertAll(
                () -> assertNotNull(instance.environment().getProperty(IEnvironment.Keys.VERSION.get()))
        );

        try {
            final Stream<Field> transformedFields = Stream.of(Class.forName("cpw.mods.modlauncher.testjar.TestClass", true, Whitebox.getInternalState(Launcher.INSTANCE, "classLoader")).getDeclaredFields());
            assertTrue(transformedFields.anyMatch(f -> f.getName().equals("testfield")), "Found transformed field");
            final Stream<Field> untransformedFields = Stream.of(Class.forName("cpw.mods.modlauncher.testjar.TestClass", true, this.getClass().getClassLoader()).getDeclaredFields());
            assertTrue(untransformedFields.noneMatch(f -> f.getName().equals("testfield")), "Didn't find transformed field");
        } catch (ClassNotFoundException e) {
            fail("Can't load class");
        }
    }
}
