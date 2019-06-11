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

import static cpw.mods.modlauncher.LogMarkers.*;

/**
 * Transformer store - holds all the transformers
 */
public class TransformStore {
    private static final Logger LOGGER = LogManager.getLogger();
    private final Set<TransformTargetLabel> classNeedsTransforming = new HashSet<>();
    private final EnumMap<TransformTargetLabel.LabelType, TransformList<?>> transformers;

    public TransformStore() {
        transformers = new EnumMap<>(TransformTargetLabel.LabelType.class);
        transformers.put(TransformTargetLabel.LabelType.CLASS, new TransformList<>(ClassNode.class));
        transformers.put(TransformTargetLabel.LabelType.METHOD, new TransformList<>(MethodNode.class));
        transformers.put(TransformTargetLabel.LabelType.FIELD, new TransformList<>(FieldNode.class));
    }

    List<ITransformer<FieldNode>> getTransformersFor(String className, FieldNode field) {
        TransformTargetLabel tl = new TransformTargetLabel(className, field.name);
        TransformList<FieldNode> transformerlist = TransformTargetLabel.LabelType.FIELD.getFromMap(this.transformers);
        return transformerlist.getTransformersForLabel(tl);
    }

    List<ITransformer<MethodNode>> getTransformersFor(String className, MethodNode method) {
        TransformTargetLabel tl = new TransformTargetLabel(className, method.name, method.desc);
        TransformList<MethodNode> transformerlist = TransformTargetLabel.LabelType.METHOD.getFromMap(this.transformers);
        return transformerlist.getTransformersForLabel(tl);
    }

    List<ITransformer<ClassNode>> getTransformersFor(String className) {
        TransformTargetLabel tl = new TransformTargetLabel(className);
        TransformList<ClassNode> transformerlist = TransformTargetLabel.LabelType.CLASS.getFromMap(this.transformers);
        return transformerlist.getTransformersForLabel(tl);
    }

    @SuppressWarnings("unchecked")
    <T> void addTransformer(TransformTargetLabel targetLabel, ITransformer<T> transformer, ITransformationService service) {
        LOGGER.debug(MODLAUNCHER,"Adding transformer {} to {}", () -> transformer, () -> targetLabel);
        classNeedsTransforming.add(new TransformTargetLabel(targetLabel.getClassName().getInternalName()));
        final TransformList<T> transformList = (TransformList<T>) this.transformers.get(targetLabel.getLabelType());
        transformList.addTransformer(targetLabel, new TransformerHolder<>(transformer, service));
    }

    boolean needsTransforming(String className) {
        return classNeedsTransforming.contains(new TransformTargetLabel(className));
    }
}
