package cpw.mods.modlauncher.test;

import cpw.mods.modlauncher.LauncherServiceMetadataDecorator;
import cpw.mods.modlauncher.TargetLabel;
import cpw.mods.modlauncher.TransformList;
import cpw.mods.modlauncher.TransformStore;
import cpw.mods.modlauncher.api.ITransformer;
import cpw.mods.modlauncher.api.IVotingContext;
import cpw.mods.modlauncher.api.VoteResult;
import org.junit.jupiter.api.Test;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodNode;
import org.powermock.reflect.Whitebox;

import javax.annotation.Nonnull;
import java.util.Collection;
import java.util.EnumMap;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LauncherServiceMetadataDecoratorTests
{

    private ClassNodeTransformer classNodeTransformer = new ClassNodeTransformer();
    private MethodNodeTransformer methodNodeTransformer = new MethodNodeTransformer();

    @Test
    void testGatherTransformersNormally() throws Exception
    {
        MockLauncherService mockLauncherService = new MockLauncherService()
        {
            @Nonnull
            @Override
            public List<ITransformer> transformers()
            {
                return Stream.of(classNodeTransformer, methodNodeTransformer).collect(Collectors.toList());
            }
        };
        TransformStore store = new TransformStore();

        LauncherServiceMetadataDecorator sd = Whitebox.invokeConstructor(LauncherServiceMetadataDecorator.class, mockLauncherService);
        sd.gatherTransformers(store);
        EnumMap<TargetLabel.LabelType, TransformList<?>> transformers = Whitebox.getInternalState(store, "transformers");
        Set<TargetLabel> targettedClasses = Whitebox.getInternalState(store, "classNeedsTransforming");
        assertAll(
                () -> assertTrue(transformers.containsKey(TargetLabel.LabelType.CLASS)),
                () -> assertTrue(transformers.get(TargetLabel.LabelType.CLASS).getTransformers().values().stream().flatMap(Collection::stream).allMatch(s -> s == classNodeTransformer)),
                () -> assertTrue(targettedClasses.contains(new TargetLabel("cheese.Puffs"))),
                () -> assertTrue(transformers.containsKey(TargetLabel.LabelType.METHOD)),
                () -> assertTrue(transformers.get(TargetLabel.LabelType.METHOD).getTransformers().values().stream().flatMap(Collection::stream).allMatch(s -> s == methodNodeTransformer)),
                () -> assertTrue(targettedClasses.contains(new TargetLabel("cheesy.PuffMethod")))
        );
    }

    private static class ClassNodeTransformer implements ITransformer<ClassNode>
    {
        @Nonnull
        @Override
        public ClassNode transform(ClassNode input, IVotingContext context)
        {
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
            return Stream.of(Target.targetClass("cheese.Puffs")).collect(Collectors.toSet());
        }
    }

    private static class MethodNodeTransformer implements ITransformer<MethodNode>
    {
        @Nonnull
        @Override
        public MethodNode transform(MethodNode input, IVotingContext context)
        {
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
            return Stream.of(Target.targetMethod("cheesy.PuffMethod", "fish", "()V")).collect(Collectors.toSet());
        }
    }
}
