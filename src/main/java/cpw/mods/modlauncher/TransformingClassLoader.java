package cpw.mods.modlauncher;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.net.URLConnection;

public class TransformingClassLoader extends ClassLoader
{
    static
    {
        ClassLoader.registerAsParallelCapable();
    }

    private final ClassTransformer classTransformer;
    private final DelegatedClassLoader delegatedClassLoader;
    private final URL specialJar;

    public TransformingClassLoader(TransformStore transformStore, File specialJar)
    {
        super();
        this.classTransformer = new ClassTransformer(transformStore);
        try
        {
            this.specialJar = specialJar.toURI().toURL();
        }
        catch (MalformedURLException e)
        {
            // Huh
            throw new RuntimeException(e);
        }
        this.delegatedClassLoader = new DelegatedClassLoader();
    }

    @Override
    protected Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException
    {
        synchronized (getClassLoadingLock(name))
        {
            try
            {
                return delegatedClassLoader.findClass(name);
            }
            catch (ClassNotFoundException | SecurityException e)
            {
                return super.loadClass(name, resolve);
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
            super(new URL[] {specialJar});
        }

        @Override
        protected Class<?> findClass(final String name) throws ClassNotFoundException
        {
            final Class<?> existingClass = super.findLoadedClass(name);
            if (existingClass != null)
            {
                return existingClass;
            }
            final String path = name.replace('.', '/').concat(".class");

            final URL classResource = findResource(path);
            byte[] classBytes;
            if (classResource != null)
            {
                try (AutoURLConnection urlConnection = new AutoURLConnection(classResource))
                {
                    final int length = urlConnection.getContentLength();
                    final InputStream is = urlConnection.getInputStream();
                    classBytes = new byte[length];
                    if (is.read(classBytes) != length) {
                        throw new ClassNotFoundException("Unable to read complete stream");
                    }
                }
                catch (IOException e)
                {
                    throw new ClassNotFoundException("blargh", e);
                }
            }
            else
            {
                classBytes = new byte[0];
            }
            classBytes = classTransformer.transform(classBytes, name);
            return defineClass(name, classBytes, 0, classBytes.length);
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
