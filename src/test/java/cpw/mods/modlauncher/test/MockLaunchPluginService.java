package cpw.mods.modlauncher.test;

import cpw.mods.modlauncher.serviceapi.ILaunchPluginService;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldNode;

import java.nio.charset.StandardCharsets;
import java.util.EnumSet;

public class MockLaunchPluginService implements ILaunchPluginService {
    @Override
    public String name() {
        return "testlaunchplugin";
    }

    private static final EnumSet<Phase> YAY = EnumSet.of(Phase.BEFORE);

    @Override
    public EnumSet<Phase> handlesClass(Type classType, boolean isEmpty) {
        return YAY;
    }

    @Override
    public boolean processClass(Phase phase, ClassNode classNode, Type classType) {
        FieldNode fn = new FieldNode(Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC, "testfield2", "Ljava/lang/String;", null, "BUTTER!");
        classNode.fields.add(fn);
        return true;
    }

    // We'll test that filtering for the Ljava/lang/String; constant pool entry used by 'testfield' (which is injected by
    // the other mock transformer) works
    // Note: This assumes we run after that transformer

    private static final byte[][] FILTER = new byte[][] {
            "Ljava/lang/String;".getBytes(StandardCharsets.UTF_8)
    };

    @Override
    public byte[][] constantsFilter(Type classType, String reason) {
        return FILTER;
    }
}
