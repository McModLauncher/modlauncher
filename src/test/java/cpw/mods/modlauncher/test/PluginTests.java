package cpw.mods.modlauncher.test;

import cpw.mods.modlauncher.serviceapi.*;
import org.junit.jupiter.api.*;
import org.objectweb.asm.*;
import org.objectweb.asm.tree.*;

import java.nio.file.*;

public class PluginTests {

    @Test
    void pluginTests() {
        @SuppressWarnings("unchecked")
        ILaunchPluginService plugin = new ILaunchPluginService() {
            @Override
            public String name() {
                return "test";
            }

            @Override
            public void addResource(final Path resource) {

            }

            @Override
            public ClassNode processClass(final ClassNode classNode, final Type classType) {
                return null;
            }

            @Override
            public String getExtension() {
                return "CHEESE";
            }

            @Override
            public boolean handlesClass(final Type classType) {
                return true;
            }
        };

        String s = plugin.getExtension();
        System.out.println(s);
    }
}
