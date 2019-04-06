package cpw.mods.modlauncher;

import cpw.mods.modlauncher.api.*;
import cpw.mods.modlauncher.serviceapi.*;
import org.apache.logging.log4j.LogManager;

import java.util.*;
import java.util.function.BiFunction;

import static cpw.mods.modlauncher.LogMarkers.*;

/**
 * Entry point for the ModLauncher.
 */
public class Launcher {
    public static Launcher INSTANCE;
    private final TypesafeMap blackboard;
    private final TransformationServicesHandler transformationServicesHandler;
    private final Environment environment;
    private final TransformStore transformStore;
    private final NameMappingServiceHandler nameMappingServiceHandler;
    private final ArgumentHandler argumentHandler;
    private final LaunchServiceHandler launchService;
    private final LaunchPluginHandler launchPlugins;
    private TransformingClassLoader classLoader;

    private Launcher() {
        INSTANCE = this;
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
        ValidateLibraries.validate();
        LogManager.getLogger().info(MODLAUNCHER,"ModLauncher running: args {}", () -> LaunchServiceHandler.hideAccessToken(args));
        new Launcher().run(args);
    }

    public final TypesafeMap blackboard() {
        return blackboard;
    }

    private void run(String... args) {
        this.argumentHandler.setArgs(args);
        this.transformationServicesHandler.initializeTransformationServices(this.argumentHandler, this.environment, this.nameMappingServiceHandler);
        this.launchService.validateLaunchTarget(this.argumentHandler);
        final TransformingClassLoaderBuilder classLoaderBuilder = this.launchService.identifyTransformationTargets(this.argumentHandler);
        this.classLoader = this.transformationServicesHandler.buildTransformingClassLoader(this.launchPlugins, classLoaderBuilder);
        Thread.currentThread().setContextClassLoader(this.classLoader);
        this.launchService.launch(this.argumentHandler, this.classLoader);
    }

    public Environment environment() {
        return this.environment;
    }

    Optional<ILaunchPluginService> findLaunchPlugin(final String name) {
        return launchPlugins.get(name);
    }

    Optional<ILaunchHandlerService> findLaunchHandler(final String name) {
        return launchService.findLaunchHandler(name);
    }

    Optional<BiFunction<INameMappingService.Domain, String, String>> findNameMapping(final String targetMapping) {
        return nameMappingServiceHandler.findNameTranslator(targetMapping);
    }
}
