package cpw.mods.modlauncher.api;

import cpw.mods.modlauncher.TransformList;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldNode;
import org.objectweb.asm.tree.MethodNode;

import java.util.Map;
import java.util.function.Supplier;
import java.util.stream.Stream;

/**
 * Specifies the target type for the {@link ITransformer.Target}. Note that the type of the transformer T
 * dictates what are acceptable targets for this transformer.
 */
public final class TargetType<T> {
    /**
     * Target a class, before field and method transforms operate. SHOULD ONLY BE USED to "replace" a complete class
     * The {@link ITransformer} T variable must refer to {@link org.objectweb.asm.tree.ClassNode}
     */
    public static final TargetType<ClassNode> PRE_CLASS = new TargetType<>("PRE_CLASS", ClassNode.class);
    /**
     * Target a class. The {@link ITransformer} T variable must refer to {@link org.objectweb.asm.tree.ClassNode}
     */
    public static final TargetType<ClassNode> CLASS = new TargetType<>("CLASS", ClassNode.class);
    /**
     * Target a method. The {@link ITransformer} T variable must refer to {@link org.objectweb.asm.tree.MethodNode}
     */
    public static final TargetType<MethodNode> METHOD = new TargetType<>("METHOD", MethodNode.class);
    /**
     * Target a field. The {@link ITransformer} T variable must refer to {@link org.objectweb.asm.tree.FieldNode}
     */
    public static final TargetType<FieldNode> FIELD = new TargetType<>("FIELD", FieldNode.class);
    
    public static final TargetType<?>[] VALUES = new TargetType<?>[] { PRE_CLASS, CLASS, METHOD, FIELD };

    private final String name;
    private final Class<T> nodeType;

    private TargetType(String name, Class<T> nodeType) {
        this.name = name;
        this.nodeType = nodeType;
    }

    public Class<T> getNodeType() {
        return this.nodeType;
    }
    
    public static TargetType<?> byName(String name) {
        return Stream.of(VALUES)
            .filter(type -> type.name.equals(name))
            .findFirst()
            .orElseThrow(() -> new IllegalArgumentException("No TargetType of name " + name + " found"));
    }

    @SuppressWarnings("unchecked")
    public TransformList<T> get(Map<TargetType<?>, TransformList<?>> transformers) {
        return (TransformList<T>) transformers.get(this);
    }

    @SuppressWarnings("unchecked")
    public Supplier<TransformList<T>> mapSupplier(Map<TargetType<?>, TransformList<?>> transformers) {
        return () -> (TransformList<T>) transformers.get(this);
    }
}
