package cpw.mods.modlauncher.test;

import cpw.mods.modlauncher.Launcher;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.MarkerManager;
import org.apache.logging.log4j.core.Filter;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.config.Configurator;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

public class LoggingTests extends LoggingStateTestBase {
    private static Stream<Arguments> testLogLevelPropertyArgs() {
        return Stream.of(
            Arguments.of(Level.ALL,
                List.of(Level.TRACE, Level.DEBUG, Level.INFO, Level.WARN, Level.ERROR, Level.FATAL),
                List.of()),
            Arguments.of(Level.TRACE,
                List.of(Level.TRACE, Level.DEBUG, Level.INFO, Level.WARN, Level.ERROR, Level.FATAL),
                List.of()),
            Arguments.of(Level.DEBUG,
                List.of(Level.DEBUG, Level.INFO, Level.WARN, Level.ERROR, Level.FATAL),
                List.of(Level.TRACE)),
            Arguments.of(Level.INFO,
                List.of(Level.INFO, Level.WARN, Level.ERROR, Level.FATAL),
                List.of(Level.TRACE, Level.DEBUG)),
            Arguments.of(Level.WARN,
                List.of(Level.WARN, Level.ERROR, Level.FATAL),
                List.of(Level.TRACE, Level.DEBUG, Level.INFO)),
            Arguments.of(Level.ERROR,
                List.of(Level.ERROR, Level.FATAL),
                List.of(Level.TRACE, Level.DEBUG, Level.INFO, Level.WARN)),
            Arguments.of(Level.FATAL,
                List.of(Level.FATAL),
                List.of(Level.TRACE, Level.DEBUG, Level.INFO, Level.WARN, Level.ERROR)),
            Arguments.of(Level.OFF,
                List.of(),
                List.of(Level.TRACE, Level.DEBUG, Level.INFO, Level.WARN, Level.ERROR, Level.FATAL)));
    }

    @ParameterizedTest
    @MethodSource("testLogLevelPropertyArgs")
    void testLogLevelProperty(Level defaultLogLevel, List<Level> enabledLevels, List<Level> disabledLevels) {
        System.setProperty("logging.loglevel.default", defaultLogLevel.name());
        Launcher.main("--version", "1.0", "--launchTarget", "mockLaunch", "--test.mods", "A,B,C,cpw.mods.modlauncher.testjar.TestClass", "--accessToken", "SUPERSECRET!", "--testLogLevel", defaultLogLevel.name());
        for (final var level : enabledLevels) {
            assertTrue(LogManager.getRootLogger().isEnabled(level));
        }
        for (final var level : disabledLevels) {
            assertFalse(LogManager.getRootLogger().isEnabled(level));
        }
    }

    private static Stream<Arguments> testLogMarkerPropertyArgs() {
        return Stream.of(
            Arguments.of("MODLAUNCHER", Level.ALL,
                List.of(Level.TRACE, Level.DEBUG, Level.INFO, Level.WARN, Level.ERROR, Level.FATAL),
                List.of()),
            Arguments.of("MODLAUNCHER", Level.TRACE,
                List.of(Level.TRACE, Level.DEBUG, Level.INFO, Level.WARN, Level.ERROR, Level.FATAL),
                List.of()),
            Arguments.of("MODLAUNCHER", Level.DEBUG,
                List.of(Level.DEBUG, Level.INFO, Level.WARN, Level.ERROR, Level.FATAL),
                List.of(Level.TRACE)),
            Arguments.of("MODLAUNCHER", Level.INFO,
                List.of(Level.INFO, Level.WARN, Level.ERROR, Level.FATAL),
                List.of(Level.TRACE, Level.DEBUG)),
            Arguments.of("MODLAUNCHER", Level.WARN,
                List.of(Level.WARN, Level.ERROR, Level.FATAL),
                List.of(Level.TRACE, Level.DEBUG, Level.INFO)),
            Arguments.of("MODLAUNCHER", Level.ERROR,
                List.of(Level.ERROR, Level.FATAL),
                List.of(Level.TRACE, Level.DEBUG, Level.INFO, Level.WARN)),
            Arguments.of("MODLAUNCHER", Level.FATAL,
                List.of(Level.FATAL),
                List.of(Level.TRACE, Level.DEBUG, Level.INFO, Level.WARN, Level.ERROR)),
            Arguments.of("MODLAUNCHER", Level.OFF,
                List.of(),
                List.of(Level.TRACE, Level.DEBUG, Level.INFO, Level.WARN, Level.ERROR, Level.FATAL)),

            Arguments.of("LAUNCHPLUGIN", Level.ALL,
                List.of(Level.TRACE, Level.DEBUG, Level.INFO, Level.WARN, Level.ERROR, Level.FATAL),
                List.of()),
            Arguments.of("LAUNCHPLUGIN", Level.TRACE,
                List.of(Level.TRACE, Level.DEBUG, Level.INFO, Level.WARN, Level.ERROR, Level.FATAL),
                List.of()),
            Arguments.of("LAUNCHPLUGIN", Level.DEBUG,
                List.of(Level.DEBUG, Level.INFO, Level.WARN, Level.ERROR, Level.FATAL),
                List.of(Level.TRACE)),
            Arguments.of("LAUNCHPLUGIN", Level.INFO,
                List.of(Level.INFO, Level.WARN, Level.ERROR, Level.FATAL),
                List.of(Level.TRACE, Level.DEBUG)),
            Arguments.of("LAUNCHPLUGIN", Level.WARN,
                List.of(Level.WARN, Level.ERROR, Level.FATAL),
                List.of(Level.TRACE, Level.DEBUG, Level.INFO)),
            Arguments.of("LAUNCHPLUGIN", Level.ERROR,
                List.of(Level.ERROR, Level.FATAL),
                List.of(Level.TRACE, Level.DEBUG, Level.INFO, Level.WARN)),
            Arguments.of("LAUNCHPLUGIN", Level.FATAL,
                List.of(Level.FATAL),
                List.of(Level.TRACE, Level.DEBUG, Level.INFO, Level.WARN, Level.ERROR)),
            Arguments.of("LAUNCHPLUGIN", Level.OFF,
                List.of(),
                List.of(Level.TRACE, Level.DEBUG, Level.INFO, Level.WARN, Level.ERROR, Level.FATAL)));
    }

    @ParameterizedTest
    @MethodSource("testLogMarkerPropertyArgs")
    void testLogMarkerProperty(String markerName, Level defaultLogLevel, List<Level> enabledLevels, List<Level> disabledLevels) {
        System.setProperty("logging.marker." + markerName, defaultLogLevel.name());
        Launcher.main("--version", "1.0", "--launchTarget", "mockLaunch", "--test.mods", "A,B,C,cpw.mods.modlauncher.testjar.TestClass", "--accessToken", "SUPERSECRET!", "--testMarker", markerName, "--testMarkerLevel", defaultLogLevel.name());
        var marker = MarkerManager.getMarker(markerName);
        for (final var level : enabledLevels) {
            assertTrue(LogManager.getRootLogger().isEnabled(level, marker));
        }
        for (final var level : disabledLevels) {
            assertFalse(LogManager.getRootLogger().isEnabled(level, marker));
        }
    }
}
