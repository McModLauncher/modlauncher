package cpw.mods.modlauncher;

import cpw.mods.modlauncher.api.ILaunchHandlerService;

import java.util.concurrent.Callable;

/**
 * Default launch handler service - will launch minecraft
 */
public class DefaultLaunchHandlerService implements ILaunchHandlerService
{
    @Override
    public String name()
    {
        return "minecraft";
    }

    @Override
    public Callable<Void> launchService(String[] arguments, ClassLoader launchClassLoader)
    {

        return () -> {
            final Class<?> mcClass = Class.forName("net.minecraft.client.main.Main", true, launchClassLoader);
            mcClass.getMethod("main", String[].class).invoke(arguments);
            return null;
        };
    }
}
