package cpw.mods.modlauncher.test;

import cpw.mods.modlauncher.api.*;

import java.io.*;
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
    public File[] identifyTransformationTargets() {
        return new File[0];
    }

    @Override
    public Callable<Void> launchService(String[] arguments, ClassLoader launchClassLoader) {
        return () -> null;
    }
}
