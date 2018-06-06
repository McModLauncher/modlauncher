package cpw.mods.modlauncher;

import cpw.mods.modlauncher.api.*;

import java.io.*;
import java.lang.reflect.*;
import java.net.*;
import java.nio.file.*;
import java.util.concurrent.*;

/**
 * Default launch handler service - will launch minecraft
 */
public class DefaultLaunchHandlerService implements ILaunchHandlerService {
    public static final String LAUNCH_PROPERTY = "minecraft.client.jar";
    public static final String LAUNCH_PATH_STRING = System.getProperty(LAUNCH_PROPERTY);

    @Override
    public String name() {
        return "minecraft";
    }

    @Override
    public Path[] identifyTransformationTargets() {
        if (LAUNCH_PATH_STRING == null) {
            throw new IllegalStateException("Missing "+ LAUNCH_PROPERTY +" environment property. Update your launcher!");
        }

        return new Path[] { FileSystems.getDefault().getPath(LAUNCH_PATH_STRING) };
    }

    @Override
    public <L extends ClassLoader & ITransformingClassLoader> Callable<Void> launchService(String[] arguments, L launchClassLoader) {

        return () -> {
            final Class<?> mcClass = Class.forName("net.minecraft.client.main.Main", true, launchClassLoader);
            final Method mcClassMethod = mcClass.getMethod("main", String[].class);
            mcClassMethod.invoke(null, (Object) arguments);
            return null;
        };
    }
}
