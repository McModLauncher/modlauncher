package cpw.mods.modlauncher;

import cpw.mods.modlauncher.serviceapi.*;

import java.io.*;
import java.lang.reflect.*;
import java.net.*;
import java.util.concurrent.*;

/**
 * Default launch handler service - will launch minecraft
 */
public class DefaultLaunchHandlerService implements ILaunchHandlerService {
    @Override
    public String name() {
        return "minecraft";
    }

    @Override
    public File[] identifyTransformationTargets() {
        final URL resource = getClass().getClassLoader().getResource("net/minecraft/client/main/Main.class");
        try {
            JarURLConnection urlConnection = (JarURLConnection) resource.openConnection();
            return new File[]{new File(urlConnection.getJarFile().getName())};
        } catch (IOException | NullPointerException e) {
            e.printStackTrace();
            return new File[0];
        }
    }

    @Override
    public Callable<Void> launchService(String[] arguments, ClassLoader launchClassLoader) {

        return () -> {
            final Class<?> mcClass = Class.forName("net.minecraft.client.main.Main", true, launchClassLoader);
            final Method mcClassMethod = mcClass.getMethod("main", String[].class);
            mcClassMethod.invoke(null, (Object) arguments);
            return null;
        };
    }
}
