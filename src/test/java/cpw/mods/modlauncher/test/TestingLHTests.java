package cpw.mods.modlauncher.test;

import cpw.mods.modlauncher.*;
import org.junit.jupiter.api.*;

import java.util.concurrent.*;

import static org.junit.jupiter.api.Assertions.assertTrue;

public class TestingLHTests {
    boolean calledback;
    @Test
    public void testTestingLaunchHandler() {
        System.setProperty("test.harness", "build/classes/java/testJars");
        System.setProperty("test.harness.callable", "cpw.mods.modlauncher.test.TestingLHTests$TestCallback");
        calledback = false;
        TestCallback.callable = () -> {
            calledback = true;
            return null;
        };
        Launcher.main("--version", "1.0", "--launchTarget", "testharness");
        assertTrue(calledback, "We got called back");
    }

    public static class TestCallback {
        private static Callable<Void> callable;
        public static Callable<Void> supplier() {
            return callable;
        }
    }
}
