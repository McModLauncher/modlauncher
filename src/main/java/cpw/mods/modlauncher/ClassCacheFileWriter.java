package cpw.mods.modlauncher;

import cpw.mods.modlauncher.api.ITransformationService;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;

public class ClassCacheFileWriter implements Runnable
{

    private final TransformationServicesHandler servicesHandler;
    private final ClassCache classCache;
    private boolean shouldRun = true;
    private boolean sleeping = false;
    final CountDownLatch latch = new CountDownLatch(1);

    ClassCacheFileWriter(TransformationServicesHandler servicesHandler, ClassCache classCache)
    {
        this.servicesHandler = servicesHandler;
        this.classCache = classCache;
        classCache.classCacheToWrite.clear();
    }

    @Override
    public void run() {
        if (!run_impl())
            classCache.invalidate();
        latch.countDown();
    }

    private boolean run_impl()
    {
        //CONFIG
        if (!Files.exists(classCache.cacheConfigFile))
        {
            BufferedWriter writer = null;
            try
            {
                Files.createFile(classCache.cacheConfigFile);
                writer = Files.newBufferedWriter(classCache.cacheConfigFile);
                writer.write("THIS IN AN AUTOMATIC GENERATED FILE - DO NOT MODIFY!");
                writer.newLine();
                writer.write(ClassCache.VERSION + "");
                writer.newLine();
                for (TransformationServiceDecorator serviceDecorator : servicesHandler.serviceLookup.values())
                {
                    ITransformationService service = serviceDecorator.getService();
                    writer.write(service.name());
                    writer.newLine();
                    writer.write(service.getConfigurationString());
                    writer.newLine();
                }
            }
            catch (IOException e)
            {
                Logging.launcherLog.info("Could not write config file - not creating class cache", e);
                return false;
            }
            finally
            {
                ClassCache.closeQuietly(writer);
            }
        }
        JarOutputStream jos = null;
        try
        {
            jos = new JarOutputStream(Files.newOutputStream(classCache.tempClassCacheFile));
            while (shouldRun && classCache.validCache)
            {
                try
                {
                    sleeping = true;
                    Thread.sleep(20 * 1000); //write the cache every 20 secs
                    sleeping = false;
                } catch (InterruptedException e)
                {
                    //ignore
                }
                sleeping = false;
                writeCache(jos);
            }
        }
        catch (IOException e)
        {
            Logging.launcherLog.warn("Error while writing class cache!");
            return false;
        }
        finally
        {
            ClassCache.closeQuietly(jos);
        }

        if (!classCache.validCache)
        {
            classCache.deleteCacheFiles();
        }
        return true;
    }

    private void writeCache(JarOutputStream outputStream) throws IOException
    {
        if (!classCache.classCacheToWrite.isEmpty())
        {
            Iterator<Map.Entry<String, byte[]>> mapIterator = classCache.classCacheToWrite.entrySet().iterator();
            while (mapIterator.hasNext())
            {
                Map.Entry<String, byte[]> next = mapIterator.next();
                String key = next.getKey();
                if (!classCache.blacklist.contains(key))
                {
                    JarEntry entry = new JarEntry(key.concat(".cache"));
                    outputStream.putNextEntry(entry);
                    outputStream.write(next.getValue());
                    outputStream.closeEntry();
                }
                mapIterator.remove();
            }
        }
    }

    void writeLast(Thread runningThread)
    {
        shouldRun = false;
        if (sleeping)
        {
            runningThread.interrupt();
        }
    }
}
