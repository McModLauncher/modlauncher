package cpw.mods.modlauncher;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

@SuppressWarnings("WeakerAccess")
public class ClassCache {
    static transient final int VERSION = 1;
    private ClassCacheReader cacheReader;
    private ClassCacheFileWriter classCacheFileWriter;
    private static Thread writerThread;

    final Path classCacheFile, cacheConfigFile, tempClassCacheFile, toCopyClassCacheFile;
    final URL classCacheURL;
    boolean validCache = true;
    Map<String, byte[]> classCacheToWrite = new ConcurrentHashMap<>();
    List<String> blacklist = new ArrayList<>();

    private ClassCache(File baseDir) {
        //noinspection ResultOfMethodCallIgnored
        baseDir.mkdirs();
        classCacheFile = Paths.get(baseDir + "/cache.jar");
        cacheConfigFile = Paths.get(baseDir + "/configuration.cfg");
        tempClassCacheFile = Paths.get(baseDir + "/cache.jar.tmp");
        toCopyClassCacheFile = Paths.get(baseDir + "/merge.tmp");
        try {
            classCacheURL = classCacheFile.toUri().toURL();
        } catch (MalformedURLException e) {
            throw new RuntimeException("Could not build class cache URL", e);
        }
    }

    /**
     * Call this whenever the class cache is no longer valid, like e.g in a dev environment.
     * ModLauncher will invalidate the cache automatically when your config string changes.
     */
    @SuppressWarnings("WeakerAccess")
    public void invalidate() {
        if (!validCache)
            return;
        Logging.launcherLog.info("The class cache has been invalidated. It will not be used!");
        validCache = false;
        deleteCacheFiles();
    }

    /**
     * Adds a class to the cache blacklist. This class will not be cached.
     * For example, the String {@code path/to/class/TestClass.class} will avoid the class {@code TestClass} in the package {@code path.to.class} to be cached
     */
    @SuppressWarnings("WeakerAccess")
    public void blacklistClass(String className) {
        if (!blacklist.contains(className))
            blacklist.add(className);
    }

    void deleteCacheFiles() {
        try {
            Files.deleteIfExists(classCacheFile);
            Files.deleteIfExists(cacheConfigFile);
            Files.deleteIfExists(tempClassCacheFile);
            Files.deleteIfExists(toCopyClassCacheFile);
        }
        catch (IOException ioe) {
            Logging.launcherLog.info("Could not delete invalid class cache. Bad things may happen at the next start! ", ioe);
            validCache = false;
        }
    }

    static ClassCache initReaderThread(TransformationServicesHandler servicesHandler, Environment environment) {
        Optional<File> mcDir = environment.getProperty(Environment.Keys.GAMEDIR.get());
        Optional<String> ver = environment.getProperty(Environment.Keys.VERSION.get());
        if (!mcDir.isPresent() || !ver.isPresent()) {
            Logging.launcherLog.warn("Cannot use class cache as the game dir / version is absent!");
            throw new RuntimeException(); //TODO handle this better
        }
        File baseDir = new File(mcDir.get() + "/classcache/" + ver.get() + "/");
        ClassCache classCache = new ClassCache(baseDir);
        classCache.cacheReader = new ClassCacheReader(servicesHandler, classCache);
        Thread readerThread = new Thread(classCache.cacheReader);
        readerThread.setDaemon(true);
        readerThread.setName("ClassCache Reader Thread");
        readerThread.start();
        return classCache;
    }

    void initWriterThread(TransformationServicesHandler servicesHandler)
    {
        if (!validCache) //Do not write if the cache has been invalidated.
            return;
        try {
            cacheReader.latch.await();
        } catch (InterruptedException e) {
            //Shrug
        }
        classCacheFileWriter = new ClassCacheFileWriter(servicesHandler, this);
        writerThread = new Thread(classCacheFileWriter);
        writerThread.setDaemon(true);
        writerThread.setName("ClassCache Writer Thread");
        writerThread.start();
        Runtime.getRuntime().addShutdownHook(new Thread(new ClassCacheShutdownHook()));
        cacheReader = null;
    }

    private class ClassCacheShutdownHook implements Runnable {

        @Override
        public void run() {
            classCacheFileWriter.writeLast(writerThread);
            if (writerThread.isAlive()) {
                try {
                    //wait 5 seconds for the write to finish. If it didn't finish by then, terminate the write
                    classCacheFileWriter.latch.await(5, TimeUnit.SECONDS);
                } catch (InterruptedException e) {
                    //Shrug
                }
            }
        }
    }

    static void closeQuietly(Closeable... closeables) {
        for (Closeable closeable : closeables) {
            if (closeable != null) {
                try {
                    closeable.close();
                } catch (IOException e) {
                    //NO OP
                }
            }
        }
    }
}
