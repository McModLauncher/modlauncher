package cpw.mods.modlauncher.test;

import cpw.mods.modlauncher.ClassTransformer;
import cpw.mods.modlauncher.TransformStore;
import cpw.mods.modlauncher.TransformTargetLabel;
import cpw.mods.modlauncher.api.accesstransformer.AccessTransformation;
import cpw.mods.modlauncher.api.accesstransformer.AccessVisibilityModifier;
import cpw.mods.modlauncher.api.accesstransformer.AccessWriteModifier;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldNode;
import org.powermock.reflect.Whitebox;

public class AccessTransformerTest {

    @Test
    void testAT() throws Exception
    {
        //Setup class
        ClassNode dummyClass = new ClassNode();
        dummyClass.superName = "java/lang/Object";
        dummyClass.version = 52;
        dummyClass.name = "test/ATTestClass";
        dummyClass.fields.add(new FieldNode(Opcodes.ACC_PRIVATE, "dummyfield", "Ljava/lang/String;", null, null));
        ClassWriter cw = new ClassWriter(Opcodes.ASM5);
        dummyClass.accept(cw);

        //Setup ATs
        final TransformStore transformStore = new TransformStore();
        final ClassTransformer classTransformer = Whitebox.invokeConstructor(ClassTransformer.class, transformStore);
        AccessTransformation fieldAT1 = new AccessTransformation(AccessVisibilityModifier.PROTECTED, AccessWriteModifier.KEEP, new TransformTargetLabel("test.ATTestClass", "dummyfield"));
        Whitebox.invokeMethod(transformStore, "addAccessTransformer", fieldAT1);
        AccessTransformation fieldAT2 = new AccessTransformation(AccessVisibilityModifier.PUBLIC, AccessWriteModifier.FINAL, new TransformTargetLabel("test.ATTestClass", "dummyfield"));
        Whitebox.invokeMethod(transformStore, "addAccessTransformer", fieldAT2);

        //transform and read class
        byte[] result = Whitebox.invokeMethod(classTransformer, "transform", new Class[] {byte[].class, String.class}, cw.toByteArray(), "test.ATTestClass");
        ClassReader cr = new ClassReader(result);
        ClassNode cn = new ClassNode();
        cr.accept(cn, 0);

        //check result
        Assertions.assertEquals(Opcodes.ACC_PUBLIC, cn.fields.get(0).access & 7); //Public
        Assertions.assertEquals(0, cn.fields.get(0).access & 16); //Nonfinal
    }
}
