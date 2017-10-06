package cpw.mods.modlauncher.serviceapi;

import java.io.*;
import java.util.concurrent.*;

/**
 * A singleton instance of this is loaded by the system to designate the launch target
 */
public interface ILaunchHandlerService {
    String name();

    File[] identifyTransformationTargets();

    Callable<Void> launchService(String[] arguments, ClassLoader launchClassLoader);
}
