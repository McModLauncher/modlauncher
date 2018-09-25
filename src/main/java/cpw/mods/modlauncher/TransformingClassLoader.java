package cpw.mods.modlauncher;

import cpw.mods.modlauncher.api.ITransformingClassLoader;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.*;
import java.net.*;
import java.nio.file.*;
import java.util.Arrays;
import java.util.List;
import java.util.function.Predicate;
import java.util.stream.*;

import static cpw.mods.modlauncher.LogMarkers.*;
import static cpw.mods.modlauncher.api.LamdbaExceptionUtils.*;

/**
 * Minecraft Class Loader
 * <p>
 * Somewhat modeled on code from https://dzone.com/articles/java-classloader-handling
 */
public class TransformingClassLoader extends ClassLoader implements ITransformingClassLoader {

    private static final Logger LOGGER = LogManager.getLogger();

    static {
        // We're capable of loading classes in parallel
        ClassLoader.registerAsParallelCapable();
    }

    private static final List<String> SKIP_PACKAGE_PREFIXES = Arrays.asList(
            "java.", "javax.", "org.objectweb.asm.", "org.apache.logging.log4j."
    );
    private final ClassTransformer classTransformer;
    private final DelegatedClassLoader delegatedClassLoader;
    private final URL[] specialJars;
    private Predicate<String> targetPackageFilter;

    public TransformingClassLoader(TransformStore transformStore, LaunchPluginHandler pluginHandler, Path... specialJars) {
        super();
        this.classTransformer = new ClassTransformer(transformStore, pluginHandler, this);
        this.specialJars = Stream.of(specialJars).map(rethrowFunction(f -> f.toUri().toURL())).toArray(URL[]::new);
        this.delegatedClassLoader = new DelegatedClassLoader();
        this.targetPackageFilter = s -> SKIP_PACKAGE_PREFIXES.stream().noneMatch(s::startsWith);
    }

    @Override
    protected Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
        synchronized (getClassLoadingLock(name)) {
            if (!targetPackageFilter.test(name)) {
                LOGGER.debug(CLASSLOADING, "Delegating to parent {}", name);
                return super.loadClass(name, resolve);
            }
            try {
                LOGGER.debug(CLASSLOADING, "Loading {}", name);
                return delegatedClassLoader.findClass(name);
            } catch (ClassNotFoundException | SecurityException e) {
                return super.loadClass(name, resolve);
            } finally {
                LOGGER.debug(CLASSLOADING, "Loaded {}", name);
            }
        }
    }

    @Override
    public void addTargetPackageFilter(Predicate<String> filter) {
        this.targetPackageFilter = this.targetPackageFilter.and(filter);
    }

    @SuppressWarnings("unchecked")
    public <T> Class<T> getClass(String name, byte[] bytes) {
        return (Class<T>) super.defineClass(name, bytes, 0, bytes.length);
    }

    @Override
    protected URL findResource(final String name) {
        return delegatedClassLoader.findResource(name);
    }

    static class AutoURLConnection implements AutoCloseable {
        private final URLConnection urlConnection;
        private final InputStream inputStream;

        AutoURLConnection(URL url) throws IOException {
            this.urlConnection = url.openConnection();
            this.inputStream = this.urlConnection.getInputStream();
        }

        @Override
        public void close() throws IOException {
            this.inputStream.close();
        }

        int getContentLength() {
            return this.urlConnection.getContentLength();
        }

        InputStream getInputStream() {
            return this.inputStream;
        }
    }

    private class DelegatedClassLoader extends URLClassLoader {
        DelegatedClassLoader() {
            super(specialJars);
        }

        @Override
        protected Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
            return TransformingClassLoader.this.loadClass(name, resolve);
        }

        @Override
        protected Class<?> findClass(final String name) throws ClassNotFoundException {
            final Class<?> existingClass = super.findLoadedClass(name);
            if (existingClass != null) {
                LOGGER.debug(CLASSLOADING, "Found existing class {}", name);
                return existingClass;
            }
            final String path = name.replace('.', '/').concat(".class");

            final URL classResource = findResource(path);
            byte[] classBytes;
            if (classResource != null) {
                try (AutoURLConnection urlConnection = new AutoURLConnection(classResource)) {
                    final int length = urlConnection.getContentLength();
                    final InputStream is = urlConnection.getInputStream();
                    classBytes = new byte[length];
                    int pos = 0, remain = length, read;
                    while ((read = is.read(classBytes, pos, remain)) != -1 && remain > 0) {
                        pos += read;
                        remain -= read;
                    }
                } catch (IOException e) {
                    LOGGER.error(CLASSLOADING,"Failed to load bytes for class {} at {}", name, classResource, e);
                    throw new ClassNotFoundException("blargh", e);
                }
            } else {
                classBytes = new byte[0];
            }
            classBytes = classTransformer.transform(classBytes, name);
            if (classBytes.length > 0) {
                LOGGER.debug(CLASSLOADING, "Loaded transform target {} from {}", name, classResource);
                return defineClass(name, classBytes, 0, classBytes.length);
            } else {
                LOGGER.debug(CLASSLOADING, "Failed to transform target {} from {}", name, classResource);
                // signal to the parent to fall back to the normal lookup
                throw new ClassNotFoundException();
            }
        }

    }
}
