package cpw.mods.modlauncher;

import cpw.mods.modlauncher.api.*;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * Holds onto a specific list of transformers targetting a particular node type
 */
@SuppressWarnings("WeakerAccess")
public class TransformList<T> {
    private final Map<TransformTargetLabel, List<ITransformer<T>>> transformers = new ConcurrentHashMap<>();
    private final Class<T> nodeType;

    TransformList(Class<T> nodeType) {
        this.nodeType = nodeType;
    }

    /**
     * Testing only
     * @return a map
     */
    private Map<TransformTargetLabel, List<ITransformer<T>>> getTransformers() {
        return transformers;
    }

    void addTransformer(TransformTargetLabel targetLabel, ITransformer<T> transformer) {
        // thread safety - compute if absent to insert the list
        transformers.computeIfAbsent(targetLabel, v -> new ArrayList<>());
        // thread safety - compute if present to mutate the list under the protection of the CHM
        transformers.computeIfPresent(targetLabel, (k,l)-> { l.add(transformer); return l;});
    }

    List<ITransformer<T>> getTransformersForLabel(TransformTargetLabel label) {
        // thread safety - compute if absent to insert the list
        return transformers.computeIfAbsent(label, v-> new ArrayList<>());
    }
}
