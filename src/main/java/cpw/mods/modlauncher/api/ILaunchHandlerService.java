package cpw.mods.modlauncher.api;

import java.util.concurrent.*;

/**
 * A singleton instance of this is loaded by the system to designate the launch target
 */
public interface ILaunchHandlerService {
    String name();

    void configureTransformationClassLoader(final ITransformingClassLoaderBuilder builder);

    Callable<Void> launchService(String[] arguments, ITransformingClassLoader launchClassLoader);
}
