package cpw.mods.modlauncher.test;

import cpw.mods.modlauncher.Launcher;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.config.ConfigurationFactory;
import org.apache.logging.log4j.core.config.ConfigurationSource;
import org.apache.logging.log4j.core.config.Configurator;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;

import java.util.Map;

public abstract class LoggingStateTestBase {
    @BeforeEach
    @AfterEach
    void EnsureCleanLoggingState() {
        final var keysToRemove = System.getProperties().keySet().stream()
            .map(entry -> (String)entry)
            .filter(entry -> entry.startsWith("logging."))
            .toList();

        keysToRemove.forEach(System::clearProperty);
        Configurator.reconfigure(ConfigurationFactory.getInstance().getConfiguration(LoggerContext.getContext(), ConfigurationSource.fromResource("log4j2.xml", Launcher.class.getClassLoader())));
    }
}
