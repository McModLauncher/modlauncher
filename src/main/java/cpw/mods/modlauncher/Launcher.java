package cpw.mods.modlauncher;

import cpw.mods.modlauncher.api.*;
import cpw.mods.modlauncher.serviceapi.*;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.nio.file.*;
import java.util.*;

import static cpw.mods.modlauncher.LogMarkers.*;

/**
 * Entry point for the ModLauncher.
 */
public enum Launcher {
    INSTANCE;

    private static final Logger LOGGER = LogManager.getLogger();
    private final TypesafeMap blackboard;
    private final TransformationServicesHandler transformationServicesHandler;
    private final Environment environment;
    private final TransformStore transformStore;
    private final NameMappingServiceHandler nameMappingServiceHandler;
    private final ArgumentHandler argumentHandler;
    private final LaunchServiceHandler launchService;
    private final LaunchPluginHandler launchPlugins;
    private TransformingClassLoader classLoader;

    Launcher() {
        LogManager.getLogger().info(MODLAUNCHER,"ModLauncher starting: java version {}", () -> System.getProperty("java.version"));
        this.launchService = new LaunchServiceHandler();
        this.blackboard = new TypesafeMap();
        this.environment = new Environment(this);
        this.transformStore = new TransformStore();
        this.transformationServicesHandler = new TransformationServicesHandler(this.transformStore);
        this.argumentHandler = new ArgumentHandler();
        this.nameMappingServiceHandler = new NameMappingServiceHandler();
        this.launchPlugins = new LaunchPluginHandler();
    }

    public static void main(String... args) {
        LOGGER.info(MODLAUNCHER,"ModLauncher running: args {}", () -> args);
        INSTANCE.run(args); // args --fml.myfmlarg1=<fish> --ll.myfunkyname=<>
    }

    public final TypesafeMap blackboard() {
        return blackboard;
    }

    private void run(String... args) {
        this.argumentHandler.setArgs(args);
        this.transformationServicesHandler.initializeTransformationServices(this.argumentHandler, this.environment);
        this.launchService.validateLaunchTarget(this.argumentHandler);
        Path[] specialJars = this.launchService.identifyTransformationTargets(this.argumentHandler);
        this.classLoader = this.transformationServicesHandler.buildTransformingClassLoader(this.launchPlugins, specialJars);
        Thread.currentThread().setContextClassLoader(this.classLoader);
        this.launchService.launch(this.argumentHandler, this.classLoader);
    }

    public Environment environment() {
        return this.environment;
    }

    public Optional<ILaunchPluginService> findLaunchPlugin(final String name) {
        return launchPlugins.get(name);
    }

    public Optional<ILaunchHandlerService> findLaunchHandler(final String name) {
        return launchService.findLaunchHandler(name);
    }
}
