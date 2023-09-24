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

package cpw.mods.modlauncher.api;

import org.jetbrains.annotations.NotNull;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldNode;
import org.objectweb.asm.tree.MethodNode;

import java.util.*;

/**
 * A transformer is injected into the modding ClassLoader. It can manipulate any item
 * it is designated to target.
 */
public interface ITransformer<T> {

    String[] DEFAULT_LABEL = {"default"};

    /**
     * Transform the input to the ITransformer's desire. The context from the last vote is
     * provided as well.
     *
     * @param input   The ASM input node, which can be mutated directly
     * @param context The voting context
     * @return An ASM node of the same type as that supplied. It will be used for subsequent
     * rounds of voting.
     */
    @NotNull
    T transform(T input, ITransformerVotingContext context);

    /**
     * Return the {@link TransformerVoteResult} for this transformer.
     * The transformer should evaluate whether or not is is a candidate to apply during
     * the round of voting in progress, represented by the context parameter.
     * How the vote works:
     * <ul>
     * <li>If the transformer wishes to be a candidate, it should return {@link TransformerVoteResult#YES}.</li>
     * <li>If the transformer wishes to exit the voting (the transformer has already
     * has its intended change applied, for example), it should return {@link TransformerVoteResult#NO}</li>
     * <li>If the transformer wishes to wait for future rounds of voting it should return
     * {@link TransformerVoteResult#DEFER}. Note that if there is <em>no</em> YES candidate, but DEFER
     * candidates remain, this is a DEFERRAL stalemate and the game will crash.</li>
     * <li>If the transformer wishes to crash the game, it should return {@link TransformerVoteResult#REJECT}.
     * This is extremely frowned upon, and should not be used except in extreme circumstances. If an
     * incompatibility is present, it should detect and handle it in the {@link ITransformationService#onLoad}
     * </li>
     * </ul>
     * After all votes from candidate transformers are collected, the NOs are removed from the
     * current set of voters, one from the set of YES voters is selected and it's {@link ITransformer#transform(Object, ITransformerVotingContext)}
     * method called. It is then removed from the set of transformers and another round is performed.
     *
     * @param context The context of the vote
     * @return A TransformerVoteResult indicating the desire of this transformer
     */
    @NotNull
    TransformerVoteResult castVote(ITransformerVotingContext context);

    /**
     * Return a set of {@link Target} identifying which elements this transformer wishes to try
     * and apply to. The {@link Target#getTargetType()} must match the T variable for the transformer
     * as documented in {@link TargetType}, other combinations will be rejected.
     *
     * @return The set of targets this transformer wishes to apply to
     */
    @NotNull
    Set<Target<T>> targets();
    
    @NotNull
    TargetType<T> getTargetType();

    /**
     * @return A string array for uniquely identifying this transformer instance within the service.
     */
    default String[] labels() {
        return DEFAULT_LABEL;
    }

    /**
     * Simple data holder indicating where the {@link ITransformer} can target.
     * @param className         The name of the class being targetted
     * @param elementName       The name of the element being targetted. This is the field name for a field,
     *                          the method name for a method. Empty string for other types
     * @param elementDescriptor The method's descriptor. Empty string for other types
     * @param targetType        The {@link TargetType} for this target - it should match the ITransformer
     *                          type variable T
     */
    record Target<T>(String className, String elementName, String elementDescriptor, TargetType<T> targetType) {
        /**
         * Convenience method returning a {@link Target} for a class
         *
         * @param className The name of the class
         * @return A target for the named class
         */
        @NotNull
        public static Target<ClassNode> targetClass(String className) {
            return new Target<>(className, "", "", TargetType.CLASS);
        }

        /**
         * Convenience method returning a {@link Target} for a class (prior to other loading operations)
         *
         * @param className The name of the class
         * @return A target for the named class
         */
        @NotNull
        public static Target<ClassNode> targetPreClass(String className) {
            return new Target<>(className, "", "", TargetType.PRE_CLASS);
        }
        /**
         * Convenience method return a {@link Target} for a method
         *
         * @param className        The name of the class containing the method
         * @param methodName       The name of the method
         * @param methodDescriptor The method's descriptor string
         * @return A target for the named method
         */
        @NotNull
        public static Target<MethodNode> targetMethod(String className, String methodName, String methodDescriptor) {
            return new Target<>(className, methodName, methodDescriptor, TargetType.METHOD);
        }

        /**
         * Convenience method returning a {@link Target} for a field
         *
         * @param className The name of the class containing the field
         * @param fieldName The name of the field
         * @return A target for the named field
         */
        @NotNull
        public static Target<FieldNode> targetField(String className, String fieldName) {
            return new Target<>(className, fieldName, "", TargetType.FIELD);
        }
    }
}
