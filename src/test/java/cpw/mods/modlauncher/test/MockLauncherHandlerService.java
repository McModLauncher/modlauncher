package cpw.mods.modlauncher.test;

import cpw.mods.modlauncher.api.*;

import java.nio.file.*;
import java.util.concurrent.*;

/**
 * Mock launch handler for testing
 */
public class MockLauncherHandlerService implements ILaunchHandlerService {
    @Override
    public String name() {
        return "mockLaunch";
    }

    @Override
    public void configureTransformationClassLoader(final ITransformingClassLoaderBuilder builder) {
        return;
    }

    @Override
    public Callable<Void> launchService(String[] arguments, ITransformingClassLoader launchClassLoader) {
        return () -> null;
    }
}
