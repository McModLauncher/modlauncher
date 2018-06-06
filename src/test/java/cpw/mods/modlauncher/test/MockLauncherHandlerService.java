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
    public Path[] identifyTransformationTargets() {
        return new Path[0];
    }

    @Override
    public Callable<Void> launchService(String[] arguments, ITransformingClassLoader launchClassLoader) {
        return () -> null;
    }
}
