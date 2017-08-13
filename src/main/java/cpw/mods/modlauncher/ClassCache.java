package cpw.mods.modlauncher;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@SuppressWarnings("WeakerAccess")
public class ClassCache {
    static transient final int VERSION = 1;
    static Path classCacheFile, cacheConfigFile, tempClassCacheFile, toCopyClassCacheFile;
    static boolean validCache = true;
    static Map<String, byte[]> classCacheToWrite = new ConcurrentHashMap<>();

    static void init(File baseDir)
    {
        //noinspection ResultOfMethodCallIgnored
        baseDir.mkdirs();
        classCacheFile = Paths.get(baseDir + "/cache.jar");
        cacheConfigFile = Paths.get(baseDir + "/configuration.cfg");
        tempClassCacheFile = Paths.get(baseDir + "/cache.jar.tmp");
        toCopyClassCacheFile = Paths.get(baseDir + "/merge.tmp");
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
            Files.deleteIfExists(classCacheFile);
            Files.deleteIfExists(cacheConfigFile);
            Files.deleteIfExists(tempClassCacheFile);
            Files.deleteIfExists(toCopyClassCacheFile);
        }
        catch (IOException ioe)
        {
            Logging.launcherLog.info("Could not delete invalid class cache. Bad things may happen at the next start! ", ioe);
            validCache = false;
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
}
