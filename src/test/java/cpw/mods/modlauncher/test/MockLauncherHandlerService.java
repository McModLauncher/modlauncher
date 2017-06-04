package cpw.mods.modlauncher.test;

import cpw.mods.modlauncher.api.ILaunchHandlerService;

import java.util.concurrent.Callable;

/**
 * Created by cpw on 03/06/17.
 */
public class MockLauncherHandlerService implements ILaunchHandlerService
{
    @Override
    public String name()
    {
        return "mockLaunch";
    }

    @Override
    public Callable<Void> launchService(String[] arguments, ClassLoader launchClassLoader)
    {
        return () -> null;
    }
}
