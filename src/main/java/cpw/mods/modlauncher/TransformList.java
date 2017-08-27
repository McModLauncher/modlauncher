package cpw.mods.modlauncher;

import cpw.mods.modlauncher.api.*;

import java.util.*;

/**
 * Holds onto a specific list of transformers targetting a particular node type
 */
@SuppressWarnings("WeakerAccess")
public class TransformList<T> {
    private final Map<TransformTargetLabel, List<ITransformer<T>>> transformers = new HashMap<>();
    private final Class<T> nodeType;

    TransformList(Class<T> nodeType) {
        this.nodeType = nodeType;
    }

    public Map<TransformTargetLabel, List<ITransformer<T>>> getTransformers() {
        return transformers;
    }

    void addTransformer(TransformTargetLabel targetLabel, ITransformer<T> transformer) {
        transformers.computeIfAbsent(targetLabel, v -> new ArrayList<>()).add(transformer);
    }

}
