package cpw.mods.modlauncher;

import java.io.File;
import java.util.concurrent.TimeUnit;

class ClassCacheHandler
{
    private static ClassCacheConfigReader configReaderInstance;
    private static ClassCacheFileWriter classCacheFileWriter;
    private static Thread readerThread, writerThread;

    static void init(TransformationServicesHandler servicesHandler, File baseDir)
    {
        ClassCache.init(baseDir);
        configReaderInstance = new ClassCacheConfigReader(servicesHandler);
        readerThread = new Thread(configReaderInstance);
        readerThread.setDaemon(true);
        readerThread.setName("ClassCache Configuration Reader");
        readerThread.start();
    }

    static void launchClassCacheWriter(TransformationServicesHandler servicesHandler)
    {
        try
        {
            configReaderInstance.latch.await(3, TimeUnit.SECONDS);
        }
        catch (InterruptedException e)
        {
            //Shrug
        }
        if (configReaderInstance.latch.getCount() > 0 && readerThread.isAlive()) //in case we timed out
        {
            //Slow IO, disable class cache...
            Logging.launcherLog.warn("VERY slow IO detected. Disabling class cache.");
            readerThread.interrupt();
            ClassCache.invalidate();
        }
        else
        {
            classCacheFileWriter = new ClassCacheFileWriter(servicesHandler);
            writerThread = new Thread(classCacheFileWriter);
            writerThread.setDaemon(true);
            writerThread.setName("ClassCache IO Thread");
            writerThread.start();
            Runtime.getRuntime().addShutdownHook(new Thread(ClassCacheHandler::finishWriting));
        }
        configReaderInstance = null;
    }

    private static void finishWriting()
    {
        if (classCacheFileWriter == null)
        {
            return;
        }
        classCacheFileWriter.writeLast(writerThread);
        if (writerThread.isAlive())
        {
            try
            {
                classCacheFileWriter.latch.await(5, TimeUnit.SECONDS);
            }
            catch (InterruptedException e)
            {
                //Shrug
            }
        }
    }
}
