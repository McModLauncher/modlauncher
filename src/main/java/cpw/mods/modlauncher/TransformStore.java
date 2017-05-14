package cpw.mods.modlauncher;

import cpw.mods.modlauncher.api.Transformer;
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
@SuppressWarnings("WeakerAccess")
public class TransformStore
{
    private final Set<TargetLabel> classNeedsTransforming = new HashSet<>();
    private final EnumMap<TargetLabel.LabelType, TransformList<?>> transformers;

    public TransformStore()
    {
        transformers = new EnumMap<>(TargetLabel.LabelType.class);
        transformers.put(TargetLabel.LabelType.CLASS, new TransformList<>(ClassNode.class));
        transformers.put(TargetLabel.LabelType.METHOD, new TransformList<>(MethodNode.class));
        transformers.put(TargetLabel.LabelType.FIELD, new TransformList<>(FieldNode.class));
    }

    List<Transformer<FieldNode>> getTransformersFor(String className, FieldNode field)
    {
        TargetLabel tl = new TargetLabel(className, field.name);
        TransformList<FieldNode> transformerlist = TargetLabel.LabelType.FIELD.getFromMap(this.transformers);
        return transformerlist.getTransformers().computeIfAbsent(tl, v -> new ArrayList<>());
    }

    List<Transformer<MethodNode>> getTransformersFor(String className, MethodNode method)
    {
        TargetLabel tl = new TargetLabel(className, method.name, method.desc);
        TransformList<MethodNode> transformerlist = TargetLabel.LabelType.METHOD.getFromMap(this.transformers);
        return transformerlist.getTransformers().computeIfAbsent(tl, v -> new ArrayList<>());
    }

    List<Transformer<ClassNode>> getTransformersFor(String className)
    {
        TargetLabel tl = new TargetLabel(className);
        TransformList<ClassNode> transformerlist = TargetLabel.LabelType.CLASS.getFromMap(this.transformers);
        return transformerlist.getTransformers().computeIfAbsent(tl, v -> new ArrayList<>());
    }

    @SuppressWarnings("unchecked")
    <T> void addTransformer(TargetLabel targetLabel, Transformer<T> transformer)
    {
        launcherLog.debug("Adding transformer {} to {}", () -> transformer, () -> targetLabel);
        classNeedsTransforming.add(new TargetLabel(targetLabel.getClassName().getInternalName()));
        final TransformList<T> transformList = (TransformList<T>)this.transformers.get(targetLabel.getLabelType());
        transformList.getTransformers().computeIfAbsent(targetLabel, v -> new ArrayList<>()).add(transformer);
    }

    public boolean needsTransforming(String className)
    {
        return classNeedsTransforming.contains(new TargetLabel(className));
    }
}
