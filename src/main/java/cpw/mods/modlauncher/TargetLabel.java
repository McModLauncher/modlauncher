package cpw.mods.modlauncher;

import cpw.mods.modlauncher.api.Transformer;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldNode;
import org.objectweb.asm.tree.MethodNode;

import java.util.EnumMap;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Supplier;

import static cpw.mods.modlauncher.TargetLabel.LabelType.CLASS;
import static cpw.mods.modlauncher.TargetLabel.LabelType.FIELD;
import static cpw.mods.modlauncher.TargetLabel.LabelType.METHOD;

/**
 * Detailed targetting information
 */
@SuppressWarnings("WeakerAccess")
public final class TargetLabel
{

    TargetLabel(Transformer.Target target)
    {
        this(target.getClassName(), target.getElementName(), target.getElementDescriptor(), LabelType.valueOf(target.getTargetType().name()));
    }

    private TargetLabel(String className, String elementName, String elementDescriptor, LabelType labelType)
    {
        this.className = Type.getObjectType(className.replaceAll("\\.", "/"));
        this.elementName = elementName;
        this.elementDescriptor = elementDescriptor.length() > 0? Type.getMethodType(elementDescriptor) : Type.VOID_TYPE;
        this.labelType = labelType;
    }

    public enum LabelType
    {
        FIELD(FieldNode.class), METHOD(MethodNode.class), CLASS(ClassNode.class);

        private final Class<?> nodeType;

        LabelType(Class<?> nodeType)
        {
            this.nodeType = nodeType;
        }

        public Class<?> getNodeType()
        {
            return nodeType;
        }

        @SuppressWarnings("unchecked")
        public <V> TransformList<V> getFromMap(EnumMap<LabelType, TransformList<?>> transformers)
        {
            return get(transformers, (Class<V>)this.nodeType);
        }

        @SuppressWarnings("unchecked")
        private <V> TransformList<V> get(EnumMap<LabelType, TransformList<?>> transformers, Class<V> type)
        {
            return (TransformList<V>)transformers.get(this);
        }

        @SuppressWarnings("unchecked")
        public <T> Supplier<TransformList<T>> mapSupplier(EnumMap<LabelType, TransformList<?>> transformers)
        {
            return () -> (TransformList<T>)transformers.get(this);
        }

        public static Optional<LabelType> getTypeFor(java.lang.reflect.Type type)
        {
            for (LabelType t : values())
            {
                if (t.nodeType.getName().equals(type.getTypeName()))
                {
                    return Optional.of(t);
                }
            }
            return Optional.empty();
        }

    }

    private final Type className;
    private final String elementName;
    private final Type elementDescriptor;
    private final LabelType labelType;

    public TargetLabel(String className, String fieldName)
    {
        this(className, fieldName, "", FIELD);
    }

    public TargetLabel(String className, String methodName, String methodDesc)
    {
        this(className, methodName, methodDesc, METHOD);
    }

    public TargetLabel(String className)
    {
        this(className, "", "", CLASS);
    }

    public final Type getClassName()
    {
        return this.className;
    }

    public final String getElementName()
    {
        return this.elementName;
    }

    public final Type getElementDescriptor()
    {
        return this.elementDescriptor;
    }

    public final LabelType getLabelType()
    {
        return this.labelType;
    }

    public int hashCode()
    {
        return Objects.hash(this.className, this.elementName, this.elementDescriptor);
    }

    @Override
    public boolean equals(Object obj)
    {
        try
        {
            TargetLabel tl = (TargetLabel)obj;
            return Objects.equals(this.className, tl.className)
                    && Objects.equals(this.elementName, tl.elementName)
                    && Objects.equals(this.elementDescriptor, tl.elementDescriptor);
        }
        catch (ClassCastException cce)
        {
            return false;
        }
    }

    @Override
    public String toString()
    {
        return "Target : " + Objects.toString(labelType) + " {" + Objects.toString(className) + "} {" + Objects.toString(elementName) + "} {" + Objects.toString(elementDescriptor) + "}";
    }
}
