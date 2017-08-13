package cpw.mods.modlauncher;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.concurrent.CountDownLatch;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.jar.JarInputStream;
import java.util.jar.JarOutputStream;
import java.util.zip.ZipException;

public class ClassCacheReader implements Runnable
{
    private final TransformationServicesHandler servicesHandler;
    final CountDownLatch latch = new CountDownLatch(1);

    ClassCacheReader(TransformationServicesHandler servicesHandler)
    {
        this.servicesHandler = servicesHandler;
    }

    @Override
    public void run()
    {
        if (!prepareCache())
            ClassCache.invalidate();
        latch.countDown(); //Notify that we`re done
    }

    /**
     * @return true if no invalidation should occur, false if the cache should be invalidated
     */
    private boolean prepareCache()
    {
        if (!ClassCache.cacheConfigFile.exists())
        {
            Logging.launcherLog.info("Creating new ClassCache as cache config file is missing!");
            ClassCache.deleteCacheFiles();
            return true;
        }
        BufferedReader reader = null;
        try
        {
            reader = new BufferedReader(new FileReader(ClassCache.cacheConfigFile));
            String serviceName = null;
            String read;
            int detectedCount = 0;
            reader.readLine(); //info line, ignore
            int saveVersion = Integer.parseInt(reader.readLine());
            if (saveVersion != ClassCache.VERSION)
                throw new RuntimeException("Invalid ClassCache version!");
            //validate that the environment did not change
            while ((read = reader.readLine()) != null)
            {
                if (serviceName == null)
                    serviceName = read;
                else
                {
                    detectedCount++;
                    TransformationServiceDecorator lookup = servicesHandler.serviceLookup.get(serviceName);
                    String config = lookup.getService().getConfigurationString();
                    if (!config.equals(read))
                        throw new RuntimeException("Configuration of service " + serviceName + " changed");
                    serviceName = null;
                }
            }
            reader.close();
            if (detectedCount != servicesHandler.serviceLookup.size())
                throw new RuntimeException("Transformation services changed!");
            //valid cache, now merge the temp into the other jar
            if (!ClassCache.classCacheFile.exists()) //just copy the tmp cache
            {
                if (ClassCache.tempClassCacheFile.exists())
                {
                    ClassCache.createIfMissing(ClassCache.classCacheFile);
                    Files.copy(ClassCache.tempClassCacheFile.toPath(), ClassCache.classCacheFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
                }
                return true;
            }
            //as java does not support adding files to a jar, we have to create a new file that merges the two files
            //and rename this to the classCacheFile
            JarInputStream tmpReader = null;
            JarInputStream jarReader = null;
            JarOutputStream jarWriter = null;
            try
            {
                ClassCache.deleteIfPresent(ClassCache.toCopyClassCacheFile);
                ClassCache.createIfMissing(ClassCache.toCopyClassCacheFile);
                tmpReader = new JarInputStream(new FileInputStream(ClassCache.tempClassCacheFile));
                jarReader = new JarInputStream(new FileInputStream(ClassCache.classCacheFile));
                jarWriter = new JarOutputStream(new FileOutputStream(ClassCache.toCopyClassCacheFile));
                if (copyJarFile(tmpReader, jarWriter, ClassCache.tempClassCacheFile)) //empty tmp file, just rename
                {
                    copyJarFile(jarReader, jarWriter, ClassCache.classCacheFile); //need to close in order to move
                    Files.move(ClassCache.toCopyClassCacheFile.toPath(), ClassCache.classCacheFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
                }
                else
                {
                    ClassCache.closeQuietly(tmpReader, jarReader, jarWriter); //need to close in order to delete
                    ClassCache.deleteIfPresent(ClassCache.toCopyClassCacheFile);
                }
            }
            catch (IOException e)
            {
                Logging.launcherLog.error("Error while merging temp file with the cache!", e);
                return false;
            }
            finally
            {
                ClassCache.closeQuietly(tmpReader, jarReader, jarWriter);
                ClassCache.deleteIfPresent(ClassCache.toCopyClassCacheFile);
            }
        }
        catch (Exception e)
        {
            Logging.launcherLog.info("Class cache invalid - rebuilding", e);
            ClassCache.closeQuietly(reader);
            ClassCache.deleteCacheFiles();
        }
        finally
        {
            ClassCache.closeQuietly(reader);
        }
        return true;
    }

    private boolean copyJarFile(JarInputStream inputStream, JarOutputStream outputStream, File from) throws IOException
    {
        JarEntry entry;
        byte[] buffer = new byte[2 * 1024]; //2 MB buffer
        JarFile tempJarFile = null;
        try
        {
            try
            {
                tempJarFile = new JarFile(from);
            }
            catch (ZipException e)
            {
                //ignore, this means temp is empty
                return false;
            }
            while ((entry = inputStream.getNextJarEntry()) != null)
            {
                outputStream.putNextEntry(entry);
                InputStream stream = null;
                try
                {
                    stream = tempJarFile.getInputStream(entry);
                    int bytesRead;
                    while ((bytesRead = stream.read(buffer)) != -1) {
                        outputStream.write(buffer, 0, bytesRead);
                    }
                    stream.close();
                    outputStream.flush();
                    outputStream.closeEntry();
                }
                finally
                {
                    ClassCache.closeQuietly(stream);
                }
            }
        }
        finally
        {
            ClassCache.closeQuietly(tempJarFile);
        }
        return true;
    }
}
