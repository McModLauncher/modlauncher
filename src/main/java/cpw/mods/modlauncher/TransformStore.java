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

package cpw.mods.modlauncher;

import cpw.mods.modlauncher.api.*;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.objectweb.asm.tree.*;

import java.util.*;
import java.util.stream.Collectors;

import static cpw.mods.modlauncher.LogMarkers.*;

/**
 * Transformer store - holds all the transformers
 */
public class TransformStore {
    private static final Logger LOGGER = LogManager.getLogger();
    private final Set<String> classNeedsTransforming = new HashSet<>();
    private final Map<TargetType<?>, TransformList<?>> transformers;

    public TransformStore() {
        transformers = new HashMap<>();
        for (TargetType<?> type : TargetType.VALUES)
            transformers.put(type, new TransformList<>(type.getNodeType()));
    }

    public List<ITransformer<FieldNode>> getTransformersFor(String className, FieldNode field) {
        TransformTargetLabel tl = new TransformTargetLabel(className, field.name);
        TransformList<FieldNode> transformerlist = TargetType.FIELD.get(this.transformers);
        return transformerlist.getTransformersForLabel(tl);
    }

    public List<ITransformer<MethodNode>> getTransformersFor(String className, MethodNode method) {
        TransformTargetLabel tl = new TransformTargetLabel(className, method.name, method.desc);
        TransformList<MethodNode> transformerlist = TargetType.METHOD.get(this.transformers);
        return transformerlist.getTransformersForLabel(tl);
    }

    public List<ITransformer<ClassNode>> getTransformersFor(String className, TargetType<ClassNode> classType) {
        TransformTargetLabel tl = new TransformTargetLabel(className, classType);
        TransformList<ClassNode> transformerlist = classType.get(this.transformers);
        return transformerlist.getTransformersForLabel(tl);
    }

    public void addTransformer(ITransformer<?> xform, ITransformationService owner) {
        final TargetType<?> targetType = xform.getTargetType();
        Objects.requireNonNull(targetType, "Transformer type must not be null");
        final Set<? extends ITransformer.Target<?>> targets = xform.targets();
        if (!targets.isEmpty()) {
            final Map<TargetType<?>, List<TransformTargetLabel>> targetTypeListMap = targets.stream()
                    .map(TransformTargetLabel::new)
                    .collect(Collectors.groupingBy(TransformTargetLabel::getTargetType));
            if (targetTypeListMap.keySet().size() > 1 || !targetTypeListMap.containsKey(targetType)) {
                LOGGER.error("Invalid target {} for transformer {}", targetType, xform);
                throw new IllegalArgumentException("The transformer contains invalid targets");
            }
            targetTypeListMap.values()
                    .stream()
                    .flatMap(Collection::stream)
                    .forEach(target -> addTransformer(target, xform, owner));
        }
    }

    @SuppressWarnings("unchecked")
    public <T> void addTransformer(TransformTargetLabel targetLabel, ITransformer<T> transformer, ITransformationService owner) {
        LOGGER.debug(MODLAUNCHER,"Adding transformer {} to {}", () -> transformer, () -> targetLabel);
        classNeedsTransforming.add(targetLabel.getClassName().getInternalName());
        final TransformList<T> transformList = (TransformList<T>) this.transformers.get(targetLabel.getTargetType());
        transformList.addTransformer(targetLabel, new TransformerHolder<>(transformer, owner));
    }

    /**
     * Requires internal class name (using '/' instead of '.')
     */
    boolean needsTransforming(String internalClassName) {
        return classNeedsTransforming.contains(internalClassName);
    }
}
