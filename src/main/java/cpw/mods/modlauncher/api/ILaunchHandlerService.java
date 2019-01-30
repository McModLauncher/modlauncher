package cpw.mods.modlauncher.api;

import java.util.concurrent.*;

/**
 * A singleton instance of this is loaded by the system to designate the launch target
 */
public interface ILaunchHandlerService {
    String name();

    void configureTransformationClassLoader(final ITransformingClassLoaderBuilder builder);

    <L extends ClassLoader & ITransformingClassLoader> Callable<Void> launchService(String[] arguments, L launchClassLoader);
}
