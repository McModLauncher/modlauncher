package cpw.mods.modlauncher.util;

import org.apache.logging.log4j.core.config.ConfigurationSource;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Stream;

public final class LoggingUtils {
    // We're duplicating these here because log4j doesn't make them easily accessible
    private static final String[] defaultConfigurationFiles = new String[] {
        "log4j2-test.properties",
        "log4j2-test.yaml", "log4j2-test.yml",
        "log4j2-test.json", "log4j2-test.jsn",
        "log4j2-test.xml",
        "log4j2.properties",
        "log4j2.yaml", "log4j2.yml",
        "log4j2.json", "log4j2.jsn",
        "log4j2.xml",
    };

    /**
     * Gets a list of {@link URI}s from the given {@link ModuleLayer}
     * following the same logic as log4j's automatic configuration.
     * @param layer The module layer to load the configuration source from.
     * @return The found configuration source, or null if one could not be
     * found.
     */
    public static List<URI> getConfigurationSources(ModuleLayer layer) {
        try {
            return Arrays.stream(defaultConfigurationFiles)
                .flatMap(file -> layer.modules().stream()
                    .flatMap(module -> module.getClassLoader().resources(file)))
                .map(LoggingUtils::toUri)
                .toList();
        } catch (RuntimeException e) {
            // TODO: do something with this
        }

        return List.of();
    }

    private static URI toUri(URL url) {
        try {
            return url.toURI();
        } catch (URISyntaxException e) {
            throw new RuntimeException(e);
        }
    }
}
