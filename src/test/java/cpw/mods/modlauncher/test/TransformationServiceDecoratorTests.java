package cpw.mods.modlauncher.test;

import cpw.mods.modlauncher.*;
import cpw.mods.modlauncher.api.*;
import org.checkerframework.checker.nullness.qual.*;
import org.junit.jupiter.api.*;
import org.objectweb.asm.tree.*;
import org.powermock.reflect.*;

import java.util.*;
import java.util.stream.*;

import static org.junit.jupiter.api.Assertions.*;

class TransformationServiceDecoratorTests {
    private final ClassNodeTransformer classNodeTransformer = new ClassNodeTransformer();
    private final MethodNodeTransformer methodNodeTransformer = new MethodNodeTransformer();

    @Test
    void testGatherTransformersNormally() throws Exception {
        MockTransformerService mockTransformerService = new MockTransformerService() {
            @NonNull
            @Override
            public List<ITransformer<?>> transformers() {
                return Stream.of(classNodeTransformer, methodNodeTransformer).collect(Collectors.toList());
            }
        };
        TransformStore store = new TransformStore();

        TransformationServiceDecorator sd = Whitebox.invokeConstructor(TransformationServiceDecorator.class, mockTransformerService);
        sd.gatherTransformers(store);
        EnumMap<TransformTargetLabel.LabelType, TransformList<?>> transformers = Whitebox.getInternalState(store, "transformers");
        Set<TransformTargetLabel> targettedClasses = Whitebox.getInternalState(store, "classNeedsTransforming");
        assertAll(
                () -> assertTrue(transformers.containsKey(TransformTargetLabel.LabelType.CLASS)),
                () -> assertTrue(transformers.get(TransformTargetLabel.LabelType.CLASS).getTransformers().values().stream().flatMap(Collection::stream).allMatch(s -> s == classNodeTransformer)),
                () -> assertTrue(targettedClasses.contains(new TransformTargetLabel("cheese.Puffs"))),
                () -> assertTrue(transformers.containsKey(TransformTargetLabel.LabelType.METHOD)),
                () -> assertTrue(transformers.get(TransformTargetLabel.LabelType.METHOD).getTransformers().values().stream().flatMap(Collection::stream).allMatch(s -> s == methodNodeTransformer)),
                () -> assertTrue(targettedClasses.contains(new TransformTargetLabel("cheesy.PuffMethod")))
        );
    }

    private static class ClassNodeTransformer implements ITransformer<ClassNode> {
        @NonNull
        @Override
        public ClassNode transform(ClassNode input, ITransformerVotingContext context) {
            return input;
        }

        @NonNull
        @Override
        public TransformerVoteResult castVote(ITransformerVotingContext context) {
            return TransformerVoteResult.YES;
        }

        @NonNull
        @Override
        public Set<Target> targets() {
            return Stream.of(Target.targetClass("cheese.Puffs")).collect(Collectors.toSet());
        }
    }

    private static class MethodNodeTransformer implements ITransformer<MethodNode> {
        @NonNull
        @Override
        public MethodNode transform(MethodNode input, ITransformerVotingContext context) {
            return input;
        }

        @NonNull
        @Override
        public TransformerVoteResult castVote(ITransformerVotingContext context) {
            return TransformerVoteResult.YES;
        }

        @NonNull
        @Override
        public Set<Target> targets() {
            return Stream.of(Target.targetMethod("cheesy.PuffMethod", "fish", "()V")).collect(Collectors.toSet());
        }
    }
}
