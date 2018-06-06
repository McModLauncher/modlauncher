package cpw.mods.modlauncher.api;

import java.util.function.Predicate;

public interface ITransformingClassLoader {
    default ClassLoader getInstance() {
        return (ClassLoader) this;
    }

    void setTargetPackageFilter(Predicate<String> filter);
}
