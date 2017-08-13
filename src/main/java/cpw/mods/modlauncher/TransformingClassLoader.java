/*
 * Modlauncher - utility to launch Minecraft-like game environments with runtime transformation
 * Copyright ©2016-2017 cpw and others
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301  USA
 */

package cpw.mods.modlauncher;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.net.URLConnection;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static cpw.mods.modlauncher.Logging.CLASSLOADING;
import static cpw.mods.modlauncher.Logging.launcherLog;
import static cpw.mods.modlauncher.api.LamdbaExceptionUtils.rethrowFunction;

/**
 * Minecraft Class Loader
 *
 * Somewhat modeled on code from https://dzone.com/articles/java-classloader-handling
 *
 */
public class TransformingClassLoader extends ClassLoader
{
    static
    {
        // We're capable of loading classes in parallel
        ClassLoader.registerAsParallelCapable();
    }

    private final ClassTransformer classTransformer;
    private final DelegatedClassLoader delegatedClassLoader;
    private final URL[] specialJars;

    public TransformingClassLoader(TransformStore transformStore, File... specialJars)
    {
        super();
        this.classTransformer = new ClassTransformer(transformStore);
        this.specialJars = Stream.of(specialJars).map(rethrowFunction(f -> f.toURI().toURL()))
                .collect(Collectors.toList()).toArray(new URL[specialJars.length]);
        this.delegatedClassLoader = new DelegatedClassLoader();
    }

    @Override
    protected Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException
    {
        synchronized (getClassLoadingLock(name))
        {
            try
            {
                launcherLog.debug(CLASSLOADING, "Loading {}", name);
                return delegatedClassLoader.findClass(name);
            }
            catch (ClassNotFoundException | SecurityException e)
            {
                return super.loadClass(name, resolve);
            }
            finally {
                launcherLog.debug(CLASSLOADING,"Loaded {}", name);
            }
        }
    }

    @SuppressWarnings("unchecked")
    public <T> Class<T> getClass(String name, byte[] bytes)
    {
        return (Class<T>)super.defineClass(name, bytes, 0, bytes.length);
    }

    private class DelegatedClassLoader extends URLClassLoader
    {
        DelegatedClassLoader()
        {
            super(specialJars);
            try
            {
                addURL(ClassCache.classCacheFile.toURI().toURL());
            }
            catch (MalformedURLException e)
            {
                Logging.launcherLog.error("Could not hook the class cache!", e);
            }
        }

        @Override
        protected Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException
        {
            return TransformingClassLoader.this.loadClass(name, resolve);
        }

        @Override
        protected Class<?> findClass(final String name) throws ClassNotFoundException
        {
            final Class<?> existingClass = super.findLoadedClass(name);
            if (existingClass != null)
            {
                launcherLog.debug(CLASSLOADING,"Found existing class {}", name);
                return existingClass;
            }
            final String path = name.replace('.', '/').concat(".class");
            byte[] classBytes;
            URL classResource;
            boolean needsTransform = classTransformer.shouldTransform(name);
            if (ClassCache.validCache && needsTransform) //try using the class cache
            {
                final String cachedPath = path.concat(".cache");
                classResource = findResource(cachedPath);
                if (classResource == null) //fallback to loading and transforming
                {
                    classResource = findResource(path);
                }
                else //transformed version available in cache
                {
                    launcherLog.debug(CLASSLOADING, "Found cached class {}, skipping transformation", name);
                    needsTransform = false;
                }
            }
            else
            {
                classResource = findResource(path);
            }

            if (classResource != null) //file not in cache and not present in jars
            {
                try (AutoURLConnection urlConnection = new AutoURLConnection(classResource))
                {
                    final int length = urlConnection.getContentLength();
                    final InputStream is = urlConnection.getInputStream();
                    classBytes = new byte[length];
                    int pos = 0, remain = length, read;
                    while ((read = is.read(classBytes, pos, remain)) != -1 && remain > 0)
                    {
                        pos += read;
                        remain -= read;
                    }
                }
                catch (IOException e)
                {
                    throw new ClassNotFoundException("An error occurred while reading the class", e);
                }
            }
            else
            {
                classBytes = new byte[0];
            }

            if (needsTransform) //Cached classes can circumvent this
            {
                classBytes = classTransformer.transform(classBytes, name);
            }

            if (classBytes.length > 0) {
                launcherLog.debug(CLASSLOADING,"Loaded transform target {} from {}", name, classResource);
                if (needsTransform) //add for writing the class cache
                {
                    ClassCache.classCacheToWrite.put(path, classBytes);
                }
                return defineClass(name, classBytes, 0, classBytes.length);
            }
            else
            {
                launcherLog.debug(CLASSLOADING,"Failed to transform target {} from {}", name, classResource);
                // signal to the parent to fall back to the normal lookup
                throw new ClassNotFoundException();
            }
        }
    }

    static class AutoURLConnection implements AutoCloseable
    {
        private final URLConnection urlConnection;
        private final InputStream inputStream;

        AutoURLConnection(URL url) throws IOException
        {
            this.urlConnection = url.openConnection();
            this.inputStream = this.urlConnection.getInputStream();
        }

        @Override
        public void close() throws IOException
        {
            this.inputStream.close();
        }

        int getContentLength()
        {
            return this.urlConnection.getContentLength();
        }

        InputStream getInputStream()
        {
            return this.inputStream;
        }
    }
}
