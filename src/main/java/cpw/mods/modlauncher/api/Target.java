package cpw.mods.modlauncher.api;

import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldNode;
import org.objectweb.asm.tree.MethodNode;

import java.util.function.Function;

/**
 *
 */
public class Target<T>
{
    private Target(String className, String beforeHash, String afterHash)
    {
        this.className = className;
        this.beforeHash = beforeHash;
        this.afterHash = afterHash;
    }

    public String getClassName()
    {
        return className;
    }

    public String getBeforeHash()
    {
        return beforeHash;
    }

    public String getAfterHash()
    {
        return afterHash;
    }

    public Transformer<T> makeTransformer(Function<T, T> function)
    {
        return new Transformer<T>() {

            @Override
            public Target<T> target()
            {
                return Target.this;
            }

            @Override
            public T transform(T input)
            {
                return function.apply(input);
            }
        };
    }

    private final String className;
    private final String beforeHash;
    private final String afterHash;

    public static final class ClassTarget extends Target<ClassNode>
    {

        public ClassTarget(String className, String beforeHash, String afterHash)
        {
            super(className, beforeHash, afterHash);
        }
    }

    public final class MethodTarget extends Target<MethodNode>
    {

        public MethodTarget(String className, String beforeHash, String afterHash)
        {
            super(className, beforeHash, afterHash);
        }
    }

    public final class FieldTarget extends Target<FieldNode>
    {

        public FieldTarget(String className, String beforeHash, String afterHash)
        {
            super(className, beforeHash, afterHash);
        }
    }

}
