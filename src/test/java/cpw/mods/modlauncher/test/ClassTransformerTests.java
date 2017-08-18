package cpw.mods.modlauncher.test;

import cpw.mods.modlauncher.*;
import cpw.mods.modlauncher.api.*;
import org.junit.jupiter.api.*;
import org.objectweb.asm.*;
import org.objectweb.asm.tree.*;
import org.powermock.reflect.*;

import javax.annotation.*;
import java.io.*;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test core transformer functionality
 */
class ClassTransformerTests {
    @Test
    void testClassTransformer() throws Exception {
        final TransformStore transformStore = new TransformStore();
        final ClassTransformer classTransformer = Whitebox.invokeConstructor(ClassTransformer.class, transformStore);

        Whitebox.invokeMethod(transformStore, "addTransformer", new TransformTargetLabel("test.MyClass"), classTransformer());
        byte[] result = Whitebox.invokeMethod(classTransformer, "transform", new Class[]{byte[].class, String.class}, new byte[0], "test.MyClass");
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
        Whitebox.invokeMethod(transformStore, "addTransformer", new TransformTargetLabel("test.DummyClass", "dummyfield"), fieldNodeTransformer1());
        byte[] result1 = Whitebox.invokeMethod(classTransformer, "transform", new Class[]{byte[].class, String.class}, cw.toByteArray(), "test.DummyClass");
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

    private ITransformer<FieldNode> fieldNodeTransformer1() {
        return new ITransformer<FieldNode>() {
            @Nonnull
            @Override
            public FieldNode transform(FieldNode input, ITransformerVotingContext context) {
                input.value = "CHEESE";
                return input;
            }

            @Nonnull
            @Override
            public TransformerVoteResult castVote(ITransformerVotingContext context) {
                return TransformerVoteResult.YES;
            }

            @Nonnull
            @Override
            public Set<Target> targets() {
                return Collections.emptySet();
            }
        };
    }

    private ITransformer<ClassNode> classTransformer() {
        return new ITransformer<ClassNode>() {
            @Nonnull
            @Override
            public ClassNode transform(ClassNode input, ITransformerVotingContext context) {
                FieldNode fn = new FieldNode(Opcodes.ACC_PUBLIC, "testfield", "Ljava/lang/String;", null, null);
                input.fields.add(fn);
                return input;
            }

            @Nonnull
            @Override
            public TransformerVoteResult castVote(ITransformerVotingContext context) {
                return TransformerVoteResult.YES;
            }

            @Nonnull
            @Override
            public Set<Target> targets() {
                return Collections.emptySet();
            }
        };
    }
}
