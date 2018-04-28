package cpw.mods.modlauncher;

import cpw.mods.modlauncher.api.*;

import java.nio.file.*;
import java.util.*;
import java.util.stream.*;

import static cpw.mods.modlauncher.Logging.*;

/**
 * Identifies the launch target and dispatches to it
 */
class LaunchServiceHandler {
    private final ServiceLoader<ILaunchHandlerService> launchHandlerServices;
    private final Map<String, LaunchServiceHandlerDecorator> launchHandlerLookup;

    public LaunchServiceHandler() {
        launchHandlerServices = ServiceLoader.load(ILaunchHandlerService.class);
        launcherLog.info(MODLAUNCHER,"Found launch services [{}]", () ->
                ServiceLoaderStreamUtils.toList(launchHandlerServices).stream().
                        map(ILaunchHandlerService::name).collect(Collectors.joining(",")));
        launchHandlerLookup = StreamSupport.stream(launchHandlerServices.spliterator(), false)
                .collect(Collectors.toMap(ILaunchHandlerService::name, LaunchServiceHandlerDecorator::new));
    }

    public Optional<ILaunchHandlerService> findLaunchHandler(final String name) {
        return Optional.ofNullable(launchHandlerLookup.getOrDefault(name, null)).map(LaunchServiceHandlerDecorator::getService);
    }
    private void launch(String target, String[] arguments, ClassLoader classLoader) {
        launchHandlerLookup.get(target).launch(arguments, classLoader);
    }

    public void launch(ArgumentHandler argumentHandler, TransformingClassLoader classLoader) {
        String launchTarget = argumentHandler.getLaunchTarget();
        String[] args = argumentHandler.buildArgumentList();
        launch(launchTarget, args, classLoader);
    }

    public Path[] identifyTransformationTargets(ArgumentHandler argumentHandler) {
        final String launchTarget = argumentHandler.getLaunchTarget();
        final Path[] transformationTargets = launchHandlerLookup.get(launchTarget).findTransformationTargets();
        final Path[] specialJar = argumentHandler.getSpecialJars();
        return Stream.concat(Arrays.stream(transformationTargets), Arrays.stream(specialJar)).toArray(Path[]::new);
    }
}
