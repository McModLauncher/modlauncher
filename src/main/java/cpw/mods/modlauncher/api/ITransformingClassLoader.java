package cpw.mods.modlauncher.api;

import java.util.function.Predicate;

public interface ITransformingClassLoader {
    default ClassLoader getInstance() {
        return (ClassLoader) this;
    }

    void addTargetPackageFilter(Predicate<String> filter);
}
