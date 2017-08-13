package cpw.mods.modlauncher;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@SuppressWarnings("WeakerAccess")
public class ClassCache {
    static transient final int VERSION = 1;
    static File classCacheFile, cacheConfigFile, tempClassCacheFile, toCopyClassCacheFile;
    static boolean validCache = true;
    static Map<String, byte[]> classCacheToWrite = new ConcurrentHashMap<>();

    static void init(File baseDir)
    {
        //noinspection ResultOfMethodCallIgnored
        baseDir.mkdirs();
        classCacheFile = new File(baseDir + "/cache.jar");
        cacheConfigFile = new File(baseDir + "/configuration.cfg");
        tempClassCacheFile = new File(baseDir + "/cache.jar.tmp");
        toCopyClassCacheFile = new File(baseDir + "/merge.tmp");
    }

    /**
     * Call this whenever the class cache is no longer valid, like e.g in a dev environment.
     * ModLauncher will invalidate the cache automatically when your config string changes.
     */
    @SuppressWarnings("WeakerAccess")
    public static void invalidate()
    {
        if (!validCache)
            return;
        Logging.launcherLog.info("The class cache has been invalidated. It will not be used!");
        validCache = false;
        deleteCacheFiles();
    }

    static void deleteCacheFiles()
    {
        try
        {
            ClassCache.deleteIfPresent(ClassCache.classCacheFile, ClassCache.cacheConfigFile, ClassCache.tempClassCacheFile);
        }
        catch (IOException ioe)
        {
            Logging.launcherLog.info("Could not delete invalid class cache. Bad things may happen at the next start! ", ioe);
            validCache = false;
        }
    }

    static void createIfMissing(File... files) throws IOException
    {
        for (File f : files)
        {
            if (!f.exists())
                if (!f.createNewFile())
                    throw new IOException();
        }
    }

    static void closeQuietly(Closeable... closeables)
    {
        for (Closeable closeable : closeables)
        {
            if (closeable != null)
            {
                try
                {
                    closeable.close();
                }
                catch (IOException e)
                {
                    //NO OP
                }
            }
        }
    }

    static void deleteIfPresent(File... files) throws IOException
    {
        for (File f : files)
        {
            if (f.exists())
                if (!f.delete())
                    throw new IOException("Could not delete file " + f);
        }
    }
}
