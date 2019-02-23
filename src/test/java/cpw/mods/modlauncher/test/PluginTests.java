package cpw.mods.modlauncher.test;

import cpw.mods.modlauncher.serviceapi.*;
import org.junit.jupiter.api.*;
import org.objectweb.asm.*;
import org.objectweb.asm.tree.*;

import java.nio.file.*;
import java.util.EnumSet;

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
            public void addResource(final Path resource, final String name) {

            }

            @Override
            public boolean processClass(final Phase phase, final ClassNode classNode, final Type classType) {
                return false;
            }

            @Override
            public String getExtension() {
                return "CHEESE";
            }

            @Override
            public EnumSet<Phase> handlesClass(final Type classType, final boolean isEmpty) {
                return EnumSet.of(Phase.BEFORE);
            }
        };

        String s = plugin.getExtension();
        System.out.println(s);
    }
}
