/*
 * ModLauncher - for launching Java programs with in-flight transformation ability.
 *
 *     Copyright (C) 2017-2019 cpw
 *
 *     This program is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU Lesser General Public License as published by
 *     the Free Software Foundation, version 3 of the License.
 *
 *     This program is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU Lesser General Public License for more details.
 *
 *     You should have received a copy of the GNU Lesser General Public License
 *     along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package cpw.mods.modlauncher;

import cpw.mods.modlauncher.api.IEnvironment;
import cpw.mods.modlauncher.api.ITransformingClassLoader;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.objectweb.asm.tree.ClassNode;

import javax.annotation.Nullable;
import java.io.*;
import java.net.*;
import java.nio.file.*;
import java.util.*;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.jar.*;

import static cpw.mods.modlauncher.LogMarkers.*;
import static cpw.mods.modlauncher.api.LamdbaExceptionUtils.*;

/**
 * Minecraft Class Loader
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
    private final Function<URLConnection, Manifest> manifestFinder;
    private Function<String,URL> classBytesFinder;
    private Predicate<String> targetPackageFilter;

    public TransformingClassLoader(TransformStore transformStore, LaunchPluginHandler pluginHandler, Path... paths) {
        this.classTransformer = new ClassTransformer(transformStore, pluginHandler, this);
        this.specialJars = Arrays.stream(paths).map(rethrowFunction(path->path.toUri().toURL())).toArray(URL[]::new);
        this.delegatedClassLoader = new DelegatedClassLoader(this);
        this.targetPackageFilter = s -> SKIP_PACKAGE_PREFIXES.stream().noneMatch(s::startsWith);
        this.classBytesFinder = input-> this.locateResource(input).orElse(null);
        this.manifestFinder = input -> this.findManifest(input).orElse(null);
    }

    TransformingClassLoader(TransformStore transformStore, LaunchPluginHandler pluginHandler, TransformingClassLoaderBuilder builder, final Environment environment) {
        super();
        TransformerAuditTrail tat = new TransformerAuditTrail();
        environment.computePropertyIfAbsent(IEnvironment.Keys.AUDITTRAIL.get(), v->tat);
        this.classTransformer = new ClassTransformer(transformStore, pluginHandler, this, tat);
        this.specialJars = builder.getSpecialJarsAsURLs();
        this.delegatedClassLoader = new DelegatedClassLoader(this);
        this.targetPackageFilter = s -> SKIP_PACKAGE_PREFIXES.stream().noneMatch(s::startsWith);
        this.classBytesFinder = alternate(builder.getClassBytesLocator(), this::locateResource);
        this.manifestFinder = alternate(builder.getManifestLocator(), this::findManifest);
    }

    private static <I, R> Function<I,R> alternate(@Nullable Function<I, Optional<R>> first, @Nullable Function<I, Optional<R>> second) {
        if (second == null) return input-> first.apply(input).orElse(null);
        if (first == null) return input-> second.apply(input).orElse(null);
        return input -> first.apply(input).orElseGet(() -> second.apply(input).orElse(null));
    }
    @Override
    protected Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
        synchronized (getClassLoadingLock(name)) {
            if (!targetPackageFilter.test(name)) {
                LOGGER.trace(CLASSLOADING, "Delegating to parent {}", name);
                return super.loadClass(name, resolve);
            }
            try {
                LOGGER.trace(CLASSLOADING, "Attempting to load {}", name);
                final Class<?> loadedClass = loadClass(name, this.classBytesFinder);
                LOGGER.trace(CLASSLOADING, "Class loaded for {}", name);
                return loadedClass;
            } catch (ClassNotFoundException | SecurityException e) {
                LOGGER.trace(CLASSLOADING, "Delegating to parent classloader {}", name);
                return super.loadClass(name, resolve);
            }
        }
    }

    public Class<?> getLoadedClass(String name) {
        return findLoadedClass(name);
    }

    @Override
    public void addTargetPackageFilter(Predicate<String> filter) {
        this.targetPackageFilter = this.targetPackageFilter.and(filter);
    }

    @SuppressWarnings("unchecked")
    public <T> Class<T> getClass(String name, byte[] bytes) {
        return (Class<T>) super.defineClass(name, bytes, 0, bytes.length);
    }

    public Class<?> loadClass(String name, Function<String,URL> classBytesFinder) throws ClassNotFoundException {
        final Class<?> existingClass = getLoadedClass(name);
        if (existingClass != null) {
            LOGGER.trace(CLASSLOADING, "Found existing class {}", name);
            return existingClass;
        }
        byte[] classBytes = delegatedClassLoader.findClass(name, classBytesFinder, "classloading");
        return defineClass(name, classBytes, 0, classBytes.length);
    }

    byte[] buildTransformedClassNodeFor(final String className, final String reason) throws ClassNotFoundException {
        return delegatedClassLoader.findClass(className, classBytesFinder, reason);
    }

    private Optional<Manifest> findManifest(URLConnection urlConnection) {
        try {
            if (urlConnection instanceof JarURLConnection) {

                return Optional.ofNullable(((JarURLConnection) urlConnection).getManifest());
            }
        } catch (IOException e) {
            // noop
        }
        return Optional.empty();
    }

    @Override
    protected URL findResource(final String name) {
        return delegatedClassLoader.findResource(name, classBytesFinder);
    }

    static class AutoURLConnection implements AutoCloseable {
        private final URLConnection urlConnection;
        private final InputStream inputStream;
        private final Function<URLConnection, Manifest> manifestFinder;

        AutoURLConnection(URL url, Function<URLConnection, Manifest> manifestFinder) throws IOException {
            this.urlConnection = url.openConnection();
            this.inputStream = this.urlConnection.getInputStream();
            this.manifestFinder = manifestFinder;
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

        Manifest getJarManifest() {
            return manifestFinder.apply(this.urlConnection);
        }
    }

    protected Optional<URL> locateResource(String path) {
        return Optional.ofNullable(delegatedClassLoader.findResource(path));
    }

    private static class DelegatedClassLoader extends URLClassLoader {
        static {
            ClassLoader.registerAsParallelCapable();
        }

        private final TransformingClassLoader tcl;

        DelegatedClassLoader(TransformingClassLoader cl) {
            super(cl.specialJars, null);
            this.tcl = cl;
        }


        @Override
        protected Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
            return tcl.loadClass(name, resolve);
        }

        Class<?> getLoadedClass(String name) {
            return findLoadedClass(name);
        }

        @Override
        protected Class<?> findClass(final String name) throws ClassNotFoundException {
            return tcl.findClass(name);
        }

        public URL findResource(final String name, Function<String,URL> byteFinder) {
            return byteFinder.apply(name);
        }

        protected byte[] findClass(final String name, Function<String,URL> classBytesFinder, final String reason) throws ClassNotFoundException {
            final String path = name.replace('.', '/').concat(".class");

            final URL classResource = classBytesFinder.apply(path);
            byte[] classBytes;
            Manifest jarManifest = null;
            if (classResource != null) {
                try (AutoURLConnection urlConnection = new AutoURLConnection(classResource, tcl.manifestFinder)) {
                    final int length = urlConnection.getContentLength();
                    final InputStream is = urlConnection.getInputStream();
                    classBytes = new byte[length];
                    int pos = 0, remain = length, read;
                    while ((read = is.read(classBytes, pos, remain)) != -1 && remain > 0) {
                        pos += read;
                        remain -= read;
                    }
                    jarManifest = urlConnection.getJarManifest();
                } catch (IOException e) {
                    LOGGER.trace(CLASSLOADING,"Failed to load bytes for class {} at {} reason {}", name, classResource, reason, e);
                    throw new ClassNotFoundException("Failed to find class bytes for "+name, e);
                }
            } else {
                classBytes = new byte[0];
            }
            classBytes = tcl.classTransformer.transform(classBytes, name, reason);
            if (classBytes.length > 0) {
                LOGGER.trace(CLASSLOADING, "Loaded transform target {} from {} reason {}", name, classResource, reason);

                // Only add the package if we have the
                if (reason.equals("classloading")) {
                    int i = name.lastIndexOf('.');
                    String pkgname = i > 0 ? name.substring(0, i) : "";
                    // Check if package already loaded.
                    tryDefinePackage(pkgname, jarManifest);
                }

                return classBytes;
            } else {
                LOGGER.trace(CLASSLOADING, "Failed to transform target {} from {}", name, classResource);
                // signal to the parent to fall back to the normal lookup
                throw new ClassNotFoundException();
            }
        }

        Package tryDefinePackage(String name, @Nullable Manifest man) throws IllegalArgumentException
        {
            if (tcl.getPackage(name) == null) {
                synchronized (this) {
                    if (tcl.getPackage(name) != null) return tcl.getPackage(name);

                    String path = name.replace('.', '/').concat("/");
                    String specTitle = null, specVersion = null, specVendor = null;
                    String implTitle = null, implVersion = null, implVendor = null;

                    if (man != null) {
                        Attributes attr = man.getAttributes(path);
                        if (attr != null) {
                            specTitle = attr.getValue(Attributes.Name.SPECIFICATION_TITLE);
                            specVersion = attr.getValue(Attributes.Name.SPECIFICATION_VERSION);
                            specVendor = attr.getValue(Attributes.Name.SPECIFICATION_VENDOR);
                            implTitle = attr.getValue(Attributes.Name.IMPLEMENTATION_TITLE);
                            implVersion = attr.getValue(Attributes.Name.IMPLEMENTATION_VERSION);
                            implVendor = attr.getValue(Attributes.Name.IMPLEMENTATION_VENDOR);
                        }
                        attr = man.getMainAttributes();
                        if (attr != null) {
                            if (specTitle == null) {
                                specTitle = attr.getValue(Attributes.Name.SPECIFICATION_TITLE);
                            }
                            if (specVersion == null) {
                                specVersion = attr.getValue(Attributes.Name.SPECIFICATION_VERSION);
                            }
                            if (specVendor == null) {
                                specVendor = attr.getValue(Attributes.Name.SPECIFICATION_VENDOR);
                            }
                            if (implTitle == null) {
                                implTitle = attr.getValue(Attributes.Name.IMPLEMENTATION_TITLE);
                            }
                            if (implVersion == null) {
                                implVersion = attr.getValue(Attributes.Name.IMPLEMENTATION_VERSION);
                            }
                            if (implVendor == null) {
                                implVendor = attr.getValue(Attributes.Name.IMPLEMENTATION_VENDOR);
                            }
                        }
                    }
                    return tcl.definePackage(name, specTitle, specVersion, specVendor, implTitle, implVersion, implVendor, null);
                }

            } else {
                return tcl.getPackage(name);
            }
        }

    }

}
