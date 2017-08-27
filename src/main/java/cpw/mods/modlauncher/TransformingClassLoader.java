package cpw.mods.modlauncher;

import java.io.*;
import java.net.*;
import java.util.stream.*;

import static cpw.mods.modlauncher.Logging.*;
import static cpw.mods.modlauncher.api.LamdbaExceptionUtils.*;

/**
 * Minecraft Class Loader
 * <p>
 * Somewhat modeled on code from https://dzone.com/articles/java-classloader-handling
 */
public class TransformingClassLoader extends ClassLoader {
    static {
        // We're capable of loading classes in parallel
        ClassLoader.registerAsParallelCapable();
    }

    private final ClassTransformer classTransformer;
    private final DelegatedClassLoader delegatedClassLoader;
    private final URL[] specialJars;

    public TransformingClassLoader(TransformStore transformStore, ClassCache classCache, File... specialJars) {
        super();
        this.classTransformer = new ClassTransformer(transformStore);
        this.specialJars = Stream.of(specialJars).map(rethrowFunction(f -> f.toURI().toURL()))
                .collect(Collectors.toList()).toArray(new URL[specialJars.length]);
        this.delegatedClassLoader = new DelegatedClassLoader(classCache);
    }

    @Override
    protected Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
        synchronized (getClassLoadingLock(name)) {
            try {
                launcherLog.debug(CLASSLOADING, "Loading {}", name);
                return delegatedClassLoader.findClass(name);
            } catch (ClassNotFoundException | SecurityException e) {
                return super.loadClass(name, resolve);
            } finally {
                launcherLog.debug(CLASSLOADING, "Loaded {}", name);
            }
        }
    }

    @SuppressWarnings("unchecked")
    public <T> Class<T> getClass(String name, byte[] bytes) {
        return (Class<T>) super.defineClass(name, bytes, 0, bytes.length);
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
        private final ClassCache cache;

        DelegatedClassLoader(ClassCache cache) {
            super(specialJars);
            this.cache = cache;
            if (cache.validCache)
            {
                addURL(cache.classCacheURL);
            }
        }

        @Override
        protected Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
            return TransformingClassLoader.this.loadClass(name, resolve);
        }

        @Override
        protected Class<?> findClass(final String name) throws ClassNotFoundException {
            final Class<?> existingClass = super.findLoadedClass(name);
            if (existingClass != null) {
                launcherLog.debug(CLASSLOADING, "Found existing class {}", name);
                return existingClass;
            }
            final String path = name.replace('.', '/').concat(".class");
            byte[] classBytes;
            URL classResource;
            boolean needsTransform = classTransformer.shouldTransform(name);
            if (cache.validCache && needsTransform) { //try using the class cache
                final String cachedPath = path.concat(".cache");
                classResource = findResource(cachedPath);
                if (classResource == null) { //fallback to loading and transforming
                    classResource = findResource(path);
                }
                else { //transformed version available in cache
                    launcherLog.debug(CLASSLOADING, "Found cached class {}, skipping transformation", name);
                    needsTransform = false;
                }
            }
            else {
                classResource = findResource(path);
            }

            if (classResource != null) { //file not in cache and not present in jars
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
                    throw new ClassNotFoundException("blargh", e);
                }
            } else {
                classBytes = new byte[0];
            }

            if (needsTransform) { //Cached classes can circumvent this
                classBytes = classTransformer.transform(classBytes, name);
            }

            if (classBytes.length > 0) {
                launcherLog.debug(CLASSLOADING,"Loaded transform target {} from {}", name, classResource);
                if (needsTransform && cache.validCache) { //add for writing the class cache
                    cache.classCacheToWrite.put(path, classBytes);
                }
                return defineClass(name, classBytes, 0, classBytes.length);
            } else {
                launcherLog.debug(CLASSLOADING, "Failed to transform target {} from {}", name, classResource);
                // signal to the parent to fall back to the normal lookup
                throw new ClassNotFoundException();
            }
        }

    }
}
