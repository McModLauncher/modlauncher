package cpw.mods.modlauncher.test;

import cpw.mods.modlauncher.ClassTransformer;
import cpw.mods.modlauncher.TargetLabel;
import cpw.mods.modlauncher.TransformStore;
import cpw.mods.modlauncher.TransformingClassLoader;
import cpw.mods.modlauncher.api.ITransformer;
import cpw.mods.modlauncher.api.IVotingContext;
import cpw.mods.modlauncher.api.VoteResult;
import org.junit.jupiter.api.Test;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldNode;
import org.powermock.reflect.Whitebox;

import javax.annotation.Nonnull;
import java.io.File;
import java.util.Collections;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ClassTransformerTests
{
    @Test
    void testClassTransformer() throws Exception
    {
        final TransformStore transformStore = new TransformStore();
        final ClassTransformer classTransformer = Whitebox.invokeConstructor(ClassTransformer.class, transformStore);

        Whitebox.invokeMethod(transformStore, "addTransformer", new TargetLabel("test.MyClass"), classTransformer());
        byte[] result = Whitebox.invokeMethod(classTransformer, "transform", new Class[] {byte[].class, String.class}, new byte[0], "test.MyClass");
        assertAll("Class loads and is valid",
                () -> assertNotNull(result),
                () -> assertNotNull(new TransformingClassLoader(transformStore, new File(".")).getClass("test.MyClass", result)),
                () ->
                {
                    ClassReader cr = new ClassReader(result);
                    ClassNode cn = new ClassNode();
                    cr.accept(cn, 0);
                    assertTrue(cn.fields.stream().anyMatch(f -> f.name.equals("testfield")));
                }
        );

        ClassNode dummyClass = new ClassNode();
        dummyClass.superName = "java/lang/Object";
        dummyClass.version = 52;
        dummyClass.name = "test/DummyClass";
        dummyClass.fields.add(new FieldNode(Opcodes.ACC_PUBLIC, "dummyfield", "Ljava/lang/String;", null, null));
        ClassWriter cw = new ClassWriter(Opcodes.ASM5);
        dummyClass.accept(cw);
        Whitebox.invokeMethod(transformStore, "addTransformer", new TargetLabel("test.DummyClass", "dummyfield"), fieldNodeTransformer1());
        byte[] result1 = Whitebox.invokeMethod(classTransformer, "transform", new Class[] {byte[].class, String.class}, cw.toByteArray(), "test.DummyClass");
        assertAll("Class loads and is valid",
                () -> assertNotNull(result1),
                () -> assertNotNull(new TransformingClassLoader(transformStore, new File(".")).getClass("test.DummyClass", result1)),
                () ->
                {
                    ClassReader cr = new ClassReader(result1);
                    ClassNode cn = new ClassNode();
                    cr.accept(cn, 0);
                    assertEquals("CHEESE", cn.fields.get(0).value);
                }
        );
    }

    private ITransformer<FieldNode> fieldNodeTransformer1()
    {
        return new ITransformer<FieldNode>()
        {
            @Nonnull
            @Override
            public FieldNode transform(FieldNode input, IVotingContext context)
            {
                input.value = "CHEESE";
                return input;
            }

            @Nonnull
            @Override
            public VoteResult castVote(IVotingContext context)
            {
                return VoteResult.YES;
            }

            @Nonnull
            @Override
            public Set<Target> targets()
            {
                return Collections.emptySet();
            }
        };
    }

    private ITransformer<ClassNode> classTransformer()
    {
        return new ITransformer<ClassNode>()
        {
            @Nonnull
            @Override
            public ClassNode transform(ClassNode input, IVotingContext context)
            {
                FieldNode fn = new FieldNode(Opcodes.ACC_PUBLIC, "testfield", "Ljava/lang/String;", null, null);
                input.fields.add(fn);
                return input;
            }

            @Nonnull
            @Override
            public VoteResult castVote(IVotingContext context)
            {
                return VoteResult.YES;
            }

            @Nonnull
            @Override
            public Set<Target> targets()
            {
                return Collections.emptySet();
            }
        };
    }
}
