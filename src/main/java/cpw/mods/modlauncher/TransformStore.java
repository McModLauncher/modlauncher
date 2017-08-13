/*
 * Modlauncher - utility to launch Minecraft-like game environments with runtime transformation
 * Copyright ©2016-2017 cpw and others
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301  USA
 */

package cpw.mods.modlauncher;

import cpw.mods.modlauncher.api.ITransformer;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldNode;
import org.objectweb.asm.tree.MethodNode;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static cpw.mods.modlauncher.Logging.launcherLog;

/**
 * Transformer store - holds all the transformers
 */
public class TransformStore
{
    private final Set<TransformTargetLabel> classNeedsTransforming = new HashSet<>();
    private final EnumMap<TransformTargetLabel.LabelType, TransformList<?>> transformers;

    public TransformStore()
    {
        transformers = new EnumMap<>(TransformTargetLabel.LabelType.class);
        transformers.put(TransformTargetLabel.LabelType.CLASS, new TransformList<>(ClassNode.class));
        transformers.put(TransformTargetLabel.LabelType.METHOD, new TransformList<>(MethodNode.class));
        transformers.put(TransformTargetLabel.LabelType.FIELD, new TransformList<>(FieldNode.class));
    }

    List<ITransformer<FieldNode>> getTransformersFor(String className, FieldNode field)
    {
        TransformTargetLabel tl = new TransformTargetLabel(className, field.name);
        TransformList<FieldNode> transformerlist = TransformTargetLabel.LabelType.FIELD.getFromMap(this.transformers);
        return transformerlist.getTransformers().computeIfAbsent(tl, v -> new ArrayList<>());
    }

    List<ITransformer<MethodNode>> getTransformersFor(String className, MethodNode method)
    {
        TransformTargetLabel tl = new TransformTargetLabel(className, method.name, method.desc);
        TransformList<MethodNode> transformerlist = TransformTargetLabel.LabelType.METHOD.getFromMap(this.transformers);
        return transformerlist.getTransformers().computeIfAbsent(tl, v -> new ArrayList<>());
    }

    List<ITransformer<ClassNode>> getTransformersFor(String className)
    {
        TransformTargetLabel tl = new TransformTargetLabel(className);
        TransformList<ClassNode> transformerlist = TransformTargetLabel.LabelType.CLASS.getFromMap(this.transformers);
        return transformerlist.getTransformers().computeIfAbsent(tl, v -> new ArrayList<>());
    }

    @SuppressWarnings("unchecked")
    <T> void addTransformer(TransformTargetLabel targetLabel, ITransformer<T> transformer)
    {
        launcherLog.debug("Adding transformer {} to {}", () -> transformer, () -> targetLabel);
        classNeedsTransforming.add(new TransformTargetLabel(targetLabel.getClassName().getInternalName()));
        final TransformList<T> transformList = (TransformList<T>)this.transformers.get(targetLabel.getLabelType());
        transformList.getTransformers().computeIfAbsent(targetLabel, v -> new ArrayList<>()).add(transformer);
    }

    boolean needsTransforming(String className)
    {
        return className.startsWith("b");//classNeedsTransforming.contains(new TransformTargetLabel(className)); TODO CHANGE THIS BACK
    }
}
