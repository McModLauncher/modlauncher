package cpw.mods.modlauncher.test;

import cpw.mods.modlauncher.*;
import cpw.mods.modlauncher.api.*;
import org.junit.jupiter.api.*;
import org.objectweb.asm.*;
import org.objectweb.asm.tree.*;
import org.powermock.reflect.*;

import javax.annotation.*;
import java.lang.reflect.Constructor;
import java.nio.file.*;
import java.util.*;
import java.util.stream.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test class loader
 */
class TransformingClassLoaderTests {
    private final ITransformer<ClassNode> classNodeTransformer = new ClassNodeTransformer();

    @Test
    void testClassLoader() throws Exception {
        MockTransformerService mockTransformerService = new MockTransformerService() {
            @Nonnull
            @Override
            public List<ITransformer> transformers() {
                return Stream.of(classNodeTransformer).collect(Collectors.toList());
            }
        };

        TransformStore transformStore = new TransformStore();
        LaunchPluginHandler lph = new LaunchPluginHandler();
        TransformationServiceDecorator sd = Whitebox.invokeConstructor(TransformationServiceDecorator.class, mockTransformerService);
        sd.gatherTransformers(transformStore);
        final Constructor<TransformingClassLoader> constructor = Whitebox.getConstructor(TransformingClassLoader.class, transformStore.getClass(), lph.getClass(), Path[].class);
        TransformingClassLoader tcl = constructor.newInstance(transformStore, lph, new Path[] {FileSystems.getDefault().getPath(".")});
        final Class<?> aClass = Class.forName("cheese.Puffs", true, tcl);
        assertEquals(Whitebox.getField(aClass, "testfield").getType(), String.class);
        assertEquals(Whitebox.getField(aClass, "testfield").get(null), "CHEESE!");

        final Class<?> newClass = tcl.loadClass("cheese.Puffs");
        assertEquals(aClass, newClass, "Class instance is the same from Class.forName and tcl.loadClass");
    }

    private static class ClassNodeTransformer implements ITransformer<ClassNode> {
        @Nonnull
        @Override
        public ClassNode transform(ClassNode input, ITransformerVotingContext context) {
            FieldNode fn = new FieldNode(Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC, "testfield", "Ljava/lang/String;", null, "CHEESE!");
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
            return Stream.of(Target.targetClass("cheese.Puffs")).collect(Collectors.toSet());
        }
    }

}
