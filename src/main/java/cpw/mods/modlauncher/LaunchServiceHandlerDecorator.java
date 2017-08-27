package cpw.mods.modlauncher;

import cpw.mods.modlauncher.api.*;

import java.io.*;

/**
 * Decorates {@link ILaunchHandlerService} for use by the system
 */
class LaunchServiceHandlerDecorator {
    private final ILaunchHandlerService service;

    public LaunchServiceHandlerDecorator(ILaunchHandlerService service) {
        this.service = service;
    }

    public void launch(String[] arguments, ClassLoader classLoader, ClassCache classCache)
    {
        try
        {
            this.service.launchService(arguments, classLoader).call();
        } catch (Exception e) {
            classCache.invalidate(); //Make sure the class cache isn't responsible for this crash
            throw new RuntimeException(e);
        }
    }

    public File[] findTransformationTargets() {
        return this.service.identifyTransformationTargets();
    }
}
