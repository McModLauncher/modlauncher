/*
 * ModLauncher - for launching Java programs with in-flight transformation ability.
 *
 *     Copyright (C) 2017-2019 cpw
 *
 *     This program is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU Lesser General Public License as published by
 *     the Free Software Foundation, version 3 of the License.
 *
 *     This program is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU Lesser General Public License for more details.
 *
 *     You should have received a copy of the GNU Lesser General Public License
 *     along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package cpw.mods.modlauncher.test;

import cpw.mods.modlauncher.TransformList;
import cpw.mods.modlauncher.TransformStore;
import cpw.mods.modlauncher.TransformTargetLabel;
import cpw.mods.modlauncher.TransformationServiceDecorator;
import cpw.mods.modlauncher.api.ITransformer;
import cpw.mods.modlauncher.api.ITransformerVotingContext;
import cpw.mods.modlauncher.api.TransformerVoteResult;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodNode;
import org.powermock.reflect.Whitebox;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TransformationServiceDecoratorTests {
    private final ClassNodeTransformer classNodeTransformer = new ClassNodeTransformer();
    private final MethodNodeTransformer methodNodeTransformer = new MethodNodeTransformer();

    @Test
    void testGatherTransformersNormally() throws Exception {
        MockTransformerService mockTransformerService = new MockTransformerService() {
            @NotNull
            @Override
            public List<ITransformer> transformers() {
                return Stream.of(classNodeTransformer, methodNodeTransformer).collect(Collectors.toList());
            }
        };
        TransformStore store = new TransformStore();

        TransformationServiceDecorator sd = Whitebox.invokeConstructor(TransformationServiceDecorator.class, mockTransformerService);
        sd.gatherTransformers(store);
        Map<TargetType<?>, TransformList<?>> transformers = Whitebox.getInternalState(store, "transformers");
        Set<String> targettedClasses = Whitebox.getInternalState(store, "classNeedsTransforming");
        assertAll(
                () -> assertTrue(transformers.containsKey(TargetType.CLASS), "transformers contains class"),
                () -> assertTrue(getTransformers(transformers.get(TargetType.CLASS)).values().stream().flatMap(Collection::stream).allMatch(s -> Whitebox.getInternalState(s,"wrapped") == classNodeTransformer), "transformers contains classTransformer"),
                () -> assertTrue(targettedClasses.contains("cheese/Puffs"), "targetted classes contains class name cheese/Puffs"),
                () -> assertTrue(transformers.containsKey(TargetType.METHOD), "transformers contains method"),
                () -> assertTrue(getTransformers(transformers.get(TargetType.METHOD)).values().stream().flatMap(Collection::stream).allMatch(s -> Whitebox.getInternalState(s,"wrapped") == methodNodeTransformer), "transformers contains methodTransformer"),
                () -> assertTrue(targettedClasses.contains("cheesy/PuffMethod"), "targetted classes contains class name cheesy/PuffMethod")
        );
    }

    private static <T> Map<TransformTargetLabel, List<ITransformer<T>>> getTransformers(TransformList<T> list) {
        try {
            return Whitebox.invokeMethod(list, "getTransformers");
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
    private static class ClassNodeTransformer implements ITransformer<ClassNode> {
        @NotNull
        @Override
        public ClassNode transform(ClassNode input, ITransformerVotingContext context) {
            return input;
        }

        @NotNull
        @Override
        public TransformerVoteResult castVote(ITransformerVotingContext context) {
            return TransformerVoteResult.YES;
        }

        @NotNull
        @Override
        public Set<Target<ClassNode>> targets() {
            return Stream.of(Target.targetClass("cheese.Puffs")).collect(Collectors.toSet());
        }

        @Override
        public TargetType<ClassNode> getTargetType() {
            return TargetType.CLASS;
        }
    }

    private static class MethodNodeTransformer implements ITransformer<MethodNode> {
        @NotNull
        @Override
        public MethodNode transform(MethodNode input, ITransformerVotingContext context) {
            return input;
        }

        @NotNull
        @Override
        public TransformerVoteResult castVote(ITransformerVotingContext context) {
            return TransformerVoteResult.YES;
        }

        @NotNull
        @Override
        public Set<Target<MethodNode>> targets() {
            return Stream.of(Target.targetMethod("cheesy.PuffMethod", "fish", "()V")).collect(Collectors.toSet());
        }

        @Override
        public TargetType<MethodNode> getTargetType() {
            return TargetType.METHOD;
        }
    }
}
