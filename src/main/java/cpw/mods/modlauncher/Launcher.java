package cpw.mods.modlauncher;

import cpw.mods.modlauncher.api.*;

import java.io.*;

import static cpw.mods.modlauncher.Logging.*;

/**
 * Entry point for the ModLauncher.
 */
public enum Launcher {
    INSTANCE;

    private final TypesafeMap blackboard;
    private final TransformationServicesHandler transformationServicesHandler;
    private final Environment environment;
    private final TransformStore transformStore;
    private final NameMappingServiceHandler nameMappingServiceHandler;
    private final ArgumentHandler argumentHandler;
    private final LaunchServiceHandler launchService;
    private TransformingClassLoader classLoader;

    Launcher() {
        launcherLog.info("ModLauncher starting: java version {}", () -> System.getProperty("java.version"));
        this.launchService = new LaunchServiceHandler();
        this.blackboard = new TypesafeMap();
        this.environment = new Environment();
        this.transformStore = new TransformStore();
        this.transformationServicesHandler = new TransformationServicesHandler(this.transformStore);
        this.argumentHandler = new ArgumentHandler();
        this.nameMappingServiceHandler = new NameMappingServiceHandler();
    }

    public static void main(String... args) {
        launcherLog.info("ModLauncher running: args {}", () -> args);
        INSTANCE.run(args); // args --fml.myfmlarg1=<fish> --ll.myfunkyname=<>
    }

    public final TypesafeMap blackboard() {
        return blackboard;
    }

    private void run(String... args) {
        this.argumentHandler.setArgs(args);
        this.transformationServicesHandler.initializeTransformationServices(this.argumentHandler, this.environment);
        File[] specialJars = this.launchService.identifyTransformationTargets(this.argumentHandler);
        this.classLoader = this.transformationServicesHandler.buildTransformingClassLoader(specialJars);
        Thread.currentThread().setContextClassLoader(this.classLoader);
        this.launchService.launch(this.argumentHandler, this.classLoader);
    }

    public Environment environment() {
        return this.environment;
    }
}
