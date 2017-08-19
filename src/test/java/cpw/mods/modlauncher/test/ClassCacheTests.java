package cpw.mods.modlauncher.test;

import cpw.mods.modlauncher.ClassCache;
import cpw.mods.modlauncher.ClassCacheFileWriter;
import cpw.mods.modlauncher.ClassCacheReader;
import cpw.mods.modlauncher.TransformStore;
import cpw.mods.modlauncher.TransformationServicesHandler;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.powermock.reflect.Whitebox;

import java.nio.file.Files;
import java.nio.file.Path;

public class ClassCacheTests {

    private static final String CLASS_NAME = "test.ClassCacheTest";

    @Test
    void testClassCache() throws Exception
    {
        final TransformStore store = new TransformStore();
        final TransformationServicesHandler transformationServiceHandler = Whitebox.invokeConstructor(TransformationServicesHandler.class, store);
        final Path baseDir = Files.createTempDirectory("classCache");
        final ClassCache classCache = Whitebox.invokeConstructor(ClassCache.class, baseDir.toFile());
        final ClassCacheReader classCacheReader = Whitebox.invokeConstructor(ClassCacheReader.class, transformationServiceHandler, classCache);
        Whitebox.getField(ClassCache.class, "cacheReader").set(classCache, classCacheReader);
        classCacheReader.run();
        Whitebox.invokeMethod(classCache, "initWriterThread", transformationServiceHandler);
        final ClassCacheFileWriter writer = (ClassCacheFileWriter) Whitebox.getField(ClassCache.class, "classCacheFileWriter").get(classCache);
        boolean validCache = Whitebox.getField(ClassCache.class, "validCache").getBoolean(classCache);
        boolean shouldRunWriter = Whitebox.getField(ClassCacheFileWriter.class, "shouldRun").getBoolean(writer);
        Assertions.assertAll("Class Cache has been set up correctly",
                () -> Assertions.assertTrue(validCache, "The class cache is valid and ready for use"),
                () -> Assertions.assertTrue(shouldRunWriter, "The class cache writer is still ready to write"));
    }
}
