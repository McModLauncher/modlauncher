package cpw.mods.modlauncher;

import cpw.mods.modlauncher.api.*;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.nio.file.*;
import java.util.*;
import java.util.stream.*;

import static cpw.mods.modlauncher.LogMarkers.*;

/**
 * Identifies the launch target and dispatches to it
 */
class LaunchServiceHandler {
    private static final Logger LOGGER = LogManager.getLogger();
    private final ServiceLoader<ILaunchHandlerService> launchHandlerServices;
    private final Map<String, LaunchServiceHandlerDecorator> launchHandlerLookup;

    public LaunchServiceHandler() {
        launchHandlerServices = ServiceLoader.load(ILaunchHandlerService.class);
        LOGGER.info(MODLAUNCHER,"Found launch services [{}]", () ->
                ServiceLoaderStreamUtils.toList(launchHandlerServices).stream().
                        map(ILaunchHandlerService::name).collect(Collectors.joining(",")));
        launchHandlerLookup = StreamSupport.stream(launchHandlerServices.spliterator(), false)
                .collect(Collectors.toMap(ILaunchHandlerService::name, LaunchServiceHandlerDecorator::new));
    }

    public Optional<ILaunchHandlerService> findLaunchHandler(final String name) {
        return Optional.ofNullable(launchHandlerLookup.getOrDefault(name, null)).map(LaunchServiceHandlerDecorator::getService);
    }

    private <L extends ClassLoader & ITransformingClassLoader> void launch(String target, String[] arguments, L classLoader) {
        LOGGER.info(MODLAUNCHER, "Launching target {} with arguments {}", target, Arrays.asList(arguments));
        launchHandlerLookup.get(target).launch(arguments, classLoader);
    }

    public void launch(ArgumentHandler argumentHandler, TransformingClassLoader classLoader) {
        String launchTarget = argumentHandler.getLaunchTarget();
        String[] args = argumentHandler.buildArgumentList();
        launch(launchTarget, args, classLoader);
    }

    Path[] identifyTransformationTargets(final ArgumentHandler argumentHandler) {
        final String launchTarget = argumentHandler.getLaunchTarget();
        final Path[] transformationTargets = launchHandlerLookup.get(launchTarget).findTransformationTargets();
        final Path[] specialJar = argumentHandler.getSpecialJars();
        return Stream.concat(Arrays.stream(transformationTargets), Arrays.stream(specialJar)).toArray(Path[]::new);
    }

    void validateLaunchTarget(final ArgumentHandler argumentHandler) {
        if (!launchHandlerLookup.containsKey(argumentHandler.getLaunchTarget())) {
            LOGGER.error(MODLAUNCHER, "Cannot find launch target {}, unable to launch",
                    argumentHandler.getLaunchTarget());
            throw new RuntimeException("Cannot find launch target");
        }
    }
}
