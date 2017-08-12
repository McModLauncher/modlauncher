package cpw.mods.modlauncher;

import javax.annotation.Nullable;
import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class ClassCache {
    static transient final int VERSION = 1;
    static File classCacheFile, cacheConfigFile, tempClassCacheFile;
    static boolean validCache = true;
    static Map<String, byte[]> classCacheToWrite = new ConcurrentHashMap<>();

    static void init(File baseDir)
    {
        baseDir.mkdirs();
        classCacheFile = new File(baseDir + "/cache.jar");
        cacheConfigFile = new File(baseDir + "/configuration.cfg");
        tempClassCacheFile = new File(baseDir + "/cache.jar.temp");
        try
        {
            if (tempClassCacheFile.exists())
            {
                Files.move(tempClassCacheFile.toPath(), classCacheFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
                deleteIfPresent(tempClassCacheFile);
            }
            createIfMissing(cacheConfigFile, tempClassCacheFile);
        }
        catch (IOException e)
        {
            Logging.launcherLog.warn("An error occurred while initializing the class cache. It will not be used!", e);
            invalidate();
        }
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
        } catch (IOException ioe)
        {
            Logging.launcherLog.info("Could not delete invalid class cache. Bad things may happen at the next start! ", ioe);
            validCache = false;
        }
    }

    private static void createIfMissing(File... files) throws IOException
    {
        for (File f : files)
        {
            if (!f.exists())
                if (!f.createNewFile())
                    throw new IOException();
        }
    }

    static void closeQuietly(@Nullable Closeable closeable)
    {
        if (closeable != null)
        {
            try
            {
                closeable.close();
            } catch (IOException e)
            {
                //NO OP
            }
        }
    }

    private static void deleteIfPresent(File... files) throws IOException
    {
        for (File f : files)
        {
            if (f.exists())
                if (!f.delete())
                    throw new IOException("Could not delete file " + f);
        }
    }
}
