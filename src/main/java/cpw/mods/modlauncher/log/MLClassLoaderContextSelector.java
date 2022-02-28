package cpw.mods.modlauncher.log;

import cpw.mods.cl.ModuleClassLoader;
import org.apache.logging.log4j.core.selector.ClassLoaderContextSelector;

/**
 * A custom context selector to avoid initializing multiple log4j contexts due to {@link ModuleClassLoader#getParent()} always returning null (as a {@link ModuleClassLoader} can have multiple parents).
 * As all {@link ModuleClassLoader}s should get the same log4j context, we just return a static string with "MCL", otherwise we use the default logic
 */
public class MLClassLoaderContextSelector extends ClassLoaderContextSelector {

    @Override
    protected String toContextMapKey(ClassLoader loader) {
        if (loader instanceof ModuleClassLoader) {
            return "MCL";
        }
        return super.toContextMapKey(loader);
    }
}
