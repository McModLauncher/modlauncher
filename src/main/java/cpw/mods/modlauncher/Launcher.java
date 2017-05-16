package cpw.mods.modlauncher;

import cpw.mods.modlauncher.api.TypesafeMap;

import static cpw.mods.modlauncher.Logging.launcherLog;

/**
 * Entry point for the ModLauncher.
 */
public enum Launcher
{
    INSTANCE;

    private final TypesafeMap blackboard;
    private final ServicesHandler servicesHandler;
    private final Environment environment;
    private final TransformStore transformStore;
    private ArgumentHandler argumentHandler;
    private TransformingClassLoader classLoader;

    public static void main(String... args)
    {
        launcherLog.info("ModLauncher running: args {}", () -> args);
        INSTANCE.run(args); // args --fml.myfmlarg1=<fish> --ll.myfunkyname=<>
    }

    Launcher()
    {
        launcherLog.info("ModLauncher starting: java version {}", () -> System.getProperty("java.version"));
        this.blackboard = new TypesafeMap();
        this.environment = new Environment();
        this.transformStore = new TransformStore();
        this.servicesHandler = new ServicesHandler(this.environment, this.transformStore);
        this.argumentHandler = new ArgumentHandler();
    }

    public final TypesafeMap blackboard()
    {
        return blackboard;
    }

    private void run(String... args)
    {
        this.argumentHandler.setArgs(args);
        this.classLoader = this.servicesHandler.initializeServices(this.argumentHandler, this.environment);
    }

    public Environment environment()
    {
        return this.environment;
    }
}
