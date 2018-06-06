package cpw.mods.modlauncher.api;

import java.nio.file.*;
import java.util.concurrent.*;

/**
 * A singleton instance of this is loaded by the system to designate the launch target
 */
public interface ILaunchHandlerService {
    String name();

    Path[] identifyTransformationTargets();

    <L extends ClassLoader & ITransformingClassLoader> Callable<Void> launchService(String[] arguments, L launchClassLoader);
}
