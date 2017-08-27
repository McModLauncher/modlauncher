package cpw.mods.modlauncher;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.concurrent.CountDownLatch;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.jar.JarInputStream;
import java.util.jar.JarOutputStream;
import java.util.zip.ZipException;

public class ClassCacheReader implements Runnable {
    final CountDownLatch latch = new CountDownLatch(1);
    private final TransformationServicesHandler servicesHandler;
    private final ClassCache classCache;

    ClassCacheReader(TransformationServicesHandler servicesHandler, ClassCache classCache) {
        this.servicesHandler = servicesHandler;
        this.classCache = classCache;
    }

    @Override
    public void run() {
        if (!prepareCache()) classCache.deleteCacheFiles();
        latch.countDown(); //Notify that we`re done
    }

    /**
     * @return true if no invalidation should occur, false if the cache should be invalidated
     */
    private boolean prepareCache() {
        if (!Files.exists(classCache.cacheConfigFile)) {
            Logging.launcherLog.info("Creating new ClassCache as cache config file is missing!");
            return false;
        }
        BufferedReader reader = null;
        try {
            reader = Files.newBufferedReader(classCache.cacheConfigFile);
            String serviceName = null;
            String read;
            int detectedCount = 0;
            reader.readLine(); //info line, ignore
            int saveVersion = Integer.parseInt(reader.readLine());
            if (saveVersion != ClassCache.VERSION) throw new RuntimeException("Invalid ClassCache version!");
            //validate that the environment did not change
            while ((read = reader.readLine()) != null) {
                if (serviceName == null) serviceName = read;
                else {
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
            if (!Files.exists(classCache.classCacheFile)) //just copy the tmp cache
            {
                if (Files.exists(classCache.tempClassCacheFile)) {
                    if (!Files.exists(classCache.classCacheFile)) Files.createFile(classCache.classCacheFile);
                    Files.copy(classCache.tempClassCacheFile, classCache.classCacheFile, StandardCopyOption.REPLACE_EXISTING);
                }
                return true;
            }
            //as java does not support adding files to a jar, we have to create a new file that merges the two files
            //and rename this to the classCacheFile
            JarInputStream tmpReader = null;
            JarInputStream jarReader = null;
            JarOutputStream jarWriter = null;
            try {
                Files.deleteIfExists(classCache.toCopyClassCacheFile);
                if (Files.exists(classCache.toCopyClassCacheFile)) Files.createFile(classCache.toCopyClassCacheFile);
                tmpReader = new JarInputStream(Files.newInputStream(classCache.tempClassCacheFile));
                jarReader = new JarInputStream(Files.newInputStream(classCache.classCacheFile));
                jarWriter = new JarOutputStream(Files.newOutputStream(classCache.toCopyClassCacheFile));
                if (copyJarFile(tmpReader, jarWriter, classCache.tempClassCacheFile)) //empty tmp file, just rename
                {
                    copyJarFile(jarReader, jarWriter, classCache.classCacheFile);
                    ClassCache.closeQuietly(tmpReader, jarReader, jarWriter); //need to close in order to move
                    Files.delete(classCache.classCacheFile);
                    Files.move(classCache.toCopyClassCacheFile, classCache.classCacheFile);
                } else {
                    ClassCache.closeQuietly(tmpReader, jarReader, jarWriter); //need to close in order to delete
                    Files.deleteIfExists(classCache.toCopyClassCacheFile);
                }
            } catch (IOException e) {
                Logging.launcherLog.error("Error while merging temp file with the cache!", e);
                return false;
            } finally {
                ClassCache.closeQuietly(tmpReader, jarReader, jarWriter);
                Files.deleteIfExists(classCache.toCopyClassCacheFile);
            }
        } catch (Exception e) {
            Logging.launcherLog.info("Class cache invalid - rebuilding", e);
            ClassCache.closeQuietly(reader);
            return false;
        } finally {
            ClassCache.closeQuietly(reader);
        }
        return true;
    }

    private boolean copyJarFile(JarInputStream inputStream, JarOutputStream outputStream, Path from) throws IOException {
        JarEntry entry;
        byte[] buffer = new byte[2 * 1024]; //2 MB buffer
        JarFile tempJarFile = null;
        try {
            try {
                tempJarFile = new JarFile(from.toFile());
            } catch (ZipException e) {
                //ignore, this means temp is empty
                return false;
            }
            while ((entry = inputStream.getNextJarEntry()) != null) {
                outputStream.putNextEntry(entry);
                InputStream stream = null;
                try {
                    stream = tempJarFile.getInputStream(entry);
                    int bytesRead;
                    while ((bytesRead = stream.read(buffer)) != -1) {
                        outputStream.write(buffer, 0, bytesRead);
                    }
                    stream.close();
                    outputStream.flush();
                    outputStream.closeEntry();
                } finally {
                    ClassCache.closeQuietly(stream);
                }
            }
        } finally {
            ClassCache.closeQuietly(tempJarFile);
        }
        return true;
    }
}
