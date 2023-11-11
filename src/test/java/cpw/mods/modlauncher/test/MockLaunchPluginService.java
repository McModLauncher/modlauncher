package cpw.mods.modlauncher.test;

import cpw.mods.modlauncher.serviceapi.ILaunchPluginService;
import org.objectweb.asm.Type;

import java.util.EnumSet;

public class MockLaunchPluginService implements ILaunchPluginService {

    @Override
    public String name() {
        return "test";
    }

    @Override
    public EnumSet<Phase> handlesClass(Type classType, boolean isEmpty) {
        return EnumSet.noneOf(Phase.class);
    }
}
