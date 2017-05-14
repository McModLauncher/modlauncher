package cpw.mods.modlauncher.test;

import cpw.mods.modlauncher.ArgumentHandler;
import cpw.mods.modlauncher.Launcher;
import cpw.mods.modlauncher.api.Environment;
import cpw.mods.modlauncher.api.LauncherService;
import joptsimple.OptionSet;
import joptsimple.OptionSpec;
import org.junit.jupiter.api.Test;
import org.powermock.reflect.Whitebox;

import java.io.File;
import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.ServiceLoader;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertIterableEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * Test overall launcher
 */
class TestLauncher
{
    @Test
    void testLauncher() throws IllegalAccessException
    {
        final List<String> testJars = Stream.of(System.getProperty("java.class.path").split(File.pathSeparator)).filter(s -> s.contains("testJars")).collect(Collectors.toList());
        String testJarPath = testJars.get(0);
        Launcher.main("--version", "1.0", "--minecraftJar", testJarPath, "--test.mods", "A,B,C,cpw.mods.modlauncher.testjar.TestClass");
        Launcher instance = Launcher.INSTANCE;
        final ServiceLoader<LauncherService> services = Whitebox.getInternalState(Whitebox.getInternalState(instance, "serviceHandler"), "launcherServices");
        final List<LauncherService> launcherServices = StreamSupport.stream(services.spliterator(), false).collect(Collectors.toList());
        assertAll("services are present and correct",
                () -> assertEquals(1, launcherServices.size(), "Found 1 service"),
                () -> assertEquals(TestLauncherService.class, launcherServices.get(0).getClass(), "Found Test Launcher Service")
        );

        final ArgumentHandler argumentHandler = Whitebox.getInternalState(instance, "argumentHandler");
        final OptionSet options = Whitebox.getInternalState(argumentHandler, "optionSet");
        Map<String, OptionSpec<?>> optionsMap = options.specs().stream().collect(Collectors.toMap(s -> s.options().stream().collect(Collectors.joining(",")), s -> s, (u, u2) -> u));

        assertAll("options are correctly setup",
                () -> assertTrue(optionsMap.containsKey("version"), "Version field is correct"),
                () -> assertTrue(optionsMap.containsKey("test.mods"), "Test service option is correct")
        );

        final TestLauncherService testLauncherService = (TestLauncherService)launcherServices.get(0);
        assertAll("test launcher service is correctly configured",
                () -> assertIterableEquals(Arrays.asList("A", "B", "C", "cpw.mods.modlauncher.testjar.TestClass"), Whitebox.getInternalState(testLauncherService, "modList"), "modlist is configured"),
                () -> assertEquals(Whitebox.getInternalState(testLauncherService, "state"), "INITIALIZED", "Initialized was called")
        );

        assertAll(
                () -> assertNotNull(instance.environment().getProperty(Environment.Keys.VERSION.get()))
        );

        try
        {
            final Stream<Field> transformedFields = Stream.of(Class.forName("cpw.mods.modlauncher.testjar.TestClass", true, Whitebox.getInternalState(Launcher.INSTANCE, "classLoader")).getDeclaredFields());
            assertTrue(transformedFields.anyMatch(f -> f.getName().equals("testfield")), "Found transformed field");
            final Stream<Field> untransformedFields = Stream.of(Class.forName("cpw.mods.modlauncher.testjar.TestClass", true, this.getClass().getClassLoader()).getDeclaredFields());
            assertTrue(untransformedFields.noneMatch(f -> f.getName().equals("testfield")), "Didn't find transformed field");
        }
        catch (ClassNotFoundException e)
        {
            fail("Can't load class");
        }
    }
}
