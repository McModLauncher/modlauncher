package cpw.mods.modlauncher;

import cpw.mods.modlauncher.api.*;

import java.io.*;
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
        launcherLog.info("Found launch services {}", () -> ServiceLoaderStreamUtils.toList(launchHandlerServices));
        launchHandlerLookup = StreamSupport.stream(launchHandlerServices.spliterator(), false)
                .collect(Collectors.toMap(ILaunchHandlerService::name, LaunchServiceHandlerDecorator::new));
    }

    private void launch(String target, String[] arguments, ClassLoader classLoader) {
        launchHandlerLookup.get(target).launch(arguments, classLoader);
    }

    public void launch(ArgumentHandler argumentHandler, TransformingClassLoader classLoader) {
        String launchTarget = argumentHandler.getLaunchTarget();
        String[] args = argumentHandler.buildArgumentList();
        launch(launchTarget, args, classLoader);
    }

    public File[] identifyTransformationTargets(ArgumentHandler argumentHandler) {
        final String launchTarget = argumentHandler.getLaunchTarget();
        final File[] transformationTargets = launchHandlerLookup.get(launchTarget).findTransformationTargets();
        final File[] specialJar = argumentHandler.getSpecialJars();
        return Stream.concat(Arrays.stream(transformationTargets), Arrays.stream(specialJar)).collect(Collectors.toList()).toArray(new File[0]);
    }
}
