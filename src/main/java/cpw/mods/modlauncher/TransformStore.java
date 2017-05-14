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
        return classNeedsTransforming.contains(new TransformTargetLabel(className));
    }
}
