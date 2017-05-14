package cpw.mods.modlauncher;

import cpw.mods.modlauncher.api.ITransformer;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Holds onto a specific list of transformers targetting a particular node type
 */
@SuppressWarnings("WeakerAccess")
public class TransformList<T>
{
    private final Map<TargetLabel, List<ITransformer<T>>> transformers = new HashMap<>();
    private final Class<T> nodeType;

    public TransformList(Class<T> nodeType)
    {
        this.nodeType = nodeType;
    }

    public Map<TargetLabel, List<ITransformer<T>>> getTransformers()
    {
        return transformers;
    }

    void addTransformer(TargetLabel targetLabel, ITransformer<T> transformer)
    {
        transformers.computeIfAbsent(targetLabel, v -> new ArrayList<>()).add(transformer);
    }

}
