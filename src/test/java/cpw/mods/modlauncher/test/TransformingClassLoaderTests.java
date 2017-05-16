package cpw.mods.modlauncher.test;

import cpw.mods.modlauncher.LauncherServiceMetadataDecorator;
import cpw.mods.modlauncher.TransformStore;
import cpw.mods.modlauncher.TransformingClassLoader;
import cpw.mods.modlauncher.api.ITransformer;
import cpw.mods.modlauncher.api.ITransformerVotingContext;
import cpw.mods.modlauncher.api.TransformerVoteResult;
import org.junit.jupiter.api.Test;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldNode;
import org.powermock.reflect.Whitebox;

import javax.annotation.Nonnull;
import java.io.File;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Test class loader
 */
class TransformingClassLoaderTests
{
    private ITransformer<ClassNode> classNodeTransformer = new ClassNodeTransformer();

    @Test
    void testClassLoader() throws Exception
    {
        MockLauncherService mockLauncherService = new MockLauncherService()
        {
            @Nonnull
            @Override
            public List<ITransformer> transformers()
            {
                return Stream.of(classNodeTransformer).collect(Collectors.toList());
            }
        };

        TransformStore transformStore = new TransformStore();
        LauncherServiceMetadataDecorator sd = Whitebox.invokeConstructor(LauncherServiceMetadataDecorator.class, mockLauncherService);
        sd.gatherTransformers(transformStore);
        TransformingClassLoader tcl = new TransformingClassLoader(transformStore, new File("."));
        final Class<?> aClass = Class.forName("cheese.Puffs", true, tcl);
        assertEquals(Whitebox.getField(aClass, "testfield").getType(), String.class);
        assertEquals(Whitebox.getField(aClass, "testfield").get(null), "CHEESE!");
    }

    private static class ClassNodeTransformer implements ITransformer<ClassNode>
    {
        @Nonnull
        @Override
        public ClassNode transform(ClassNode input, ITransformerVotingContext context)
        {
            FieldNode fn = new FieldNode(Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC, "testfield", "Ljava/lang/String;", null, "CHEESE!");
            input.fields.add(fn);
            return input;
        }

        @Nonnull
        @Override
        public TransformerVoteResult castVote(ITransformerVotingContext context)
        {
            return TransformerVoteResult.YES;
        }

        @Nonnull
        @Override
        public Set<Target> targets()
        {
            return Stream.of(Target.targetClass("cheese.Puffs")).collect(Collectors.toSet());
        }
    }

}
