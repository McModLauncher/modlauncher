package cpw.mods.modlauncher;

import cpw.mods.modlauncher.api.ITransformer;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TransformList<T>
{
    private final Map<TransformTargetLabel, List<ITransformer<T>>> transformers = new HashMap<>();
    private final Class<T> nodeType;

    TransformList(Class<T> nodeType)
    {
        this.nodeType = nodeType;
    }

    public Map<TransformTargetLabel, List<ITransformer<T>>> getTransformers()
    {
        return transformers;
    }

    void addTransformer(TransformTargetLabel targetLabel, ITransformer<T> transformer)
    {
        transformers.computeIfAbsent(targetLabel, v -> new ArrayList<>()).add(transformer);
    }

}
