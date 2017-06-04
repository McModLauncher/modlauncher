package cpw.mods.modlauncher.api;

import java.util.concurrent.Callable;

/**
 * A singleton instance of this is loaded by the system to designate the launch target
 */
public interface ILaunchHandlerService
{
    String name();

    Callable<Void> launchService(String[] arguments, ClassLoader launchClassLoader);
}
