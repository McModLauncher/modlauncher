package cpw.mods.modlauncher;

import cpw.mods.modlauncher.api.*;

import java.nio.file.*;

/**
 * Decorates {@link ILaunchHandlerService} for use by the system
 */
class LaunchServiceHandlerDecorator {
    private final ILaunchHandlerService service;

    public LaunchServiceHandlerDecorator(ILaunchHandlerService service) {
        this.service = service;
    }

    public void launch(String[] arguments, ClassLoader classLoader) {
        try {
            this.service.launchService(arguments, classLoader).call();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public Path[] findTransformationTargets() {
        return this.service.identifyTransformationTargets();
    }
}
