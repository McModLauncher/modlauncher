package cpw.mods.modlauncher;

import java.io.BufferedReader;
import java.io.FileReader;
import java.util.concurrent.CountDownLatch;

public class ClassCacheConfigReader implements Runnable
{
    private final TransformationServicesHandler servicesHandler;
    final CountDownLatch latch = new CountDownLatch(1);

    ClassCacheConfigReader(TransformationServicesHandler servicesHandler)
    {
        this.servicesHandler = servicesHandler;
    }

    @Override
    public void run()
    {
        if (!ClassCache.cacheConfigFile.exists())
        {
            Logging.launcherLog.info("Skipping ClassCache as cache file is missing!");
            ClassCache.deleteCacheFiles();
            latch.countDown(); //Notify that we`re done
            return;
        }
        BufferedReader reader = null;
        try
        {
            reader = new BufferedReader(new FileReader(ClassCache.cacheConfigFile));
            String serviceName = null;
            String read;
            int detectedCount = 0;
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
            if (detectedCount != servicesHandler.serviceLookup.size())
                throw new RuntimeException("Transformation services changed!");
            //valid cache, inject cache jar into classloader
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
        latch.countDown(); //Notify that we`re done
    }
}
