package cpw.mods.modlauncher.test;

import cpw.mods.modlauncher.ClassCache;
import cpw.mods.modlauncher.ClassCacheFileWriter;
import cpw.mods.modlauncher.ClassCacheReader;
import cpw.mods.modlauncher.TransformStore;
import cpw.mods.modlauncher.TransformationServiceDecorator;
import cpw.mods.modlauncher.TransformationServicesHandler;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.powermock.reflect.Whitebox;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

public class ClassCacheTests {


    @SuppressWarnings("unchecked")
    @Test
    void testClassCache() throws Exception
    {
        final TransformStore store = new TransformStore();
        final TransformationServicesHandler transformationServiceHandler = Whitebox.invokeConstructor(TransformationServicesHandler.class, store);
        final Path baseDir = Files.createTempDirectory("classCache");
        final ClassCache classCache = Whitebox.invokeConstructor(ClassCache.class, baseDir.toFile());
        final ClassCacheReader classCacheReader = Whitebox.invokeConstructor(ClassCacheReader.class, transformationServiceHandler, classCache);
        Whitebox.getField(ClassCache.class, "cacheReader").set(classCache, classCacheReader);
        final ClassCacheFileWriter writer = Whitebox.invokeConstructor(ClassCacheFileWriter.class, transformationServiceHandler, classCache);
        final boolean success = (boolean) Whitebox.getMethod(ClassCacheFileWriter.class, "setupCache").invoke(writer);
        classCacheReader.run();
        final boolean validCache = Whitebox.getField(ClassCache.class, "validCache").getBoolean(classCache);
        final boolean shouldRunWriter = Whitebox.getField(ClassCacheFileWriter.class, "shouldRun").getBoolean(writer);
        Assertions.assertAll("Class Cache Config has been set up correctly",
                () -> Assertions.assertTrue(success, "The config file has been written successfully"),
                () -> Assertions.assertTrue(validCache, "The class cache is valid and ready for use"),
                () -> Assertions.assertTrue(shouldRunWriter, "The class cache writer is still ready to write"));
        TransformationServiceDecorator decorator = (TransformationServiceDecorator)((Map) Whitebox.getField(TransformationServicesHandler.class, "serviceLookup").get(transformationServiceHandler)).get("test");
        ((MockTransformerService) decorator.getService()).configString = "V2.0";
        classCacheReader.run();
        final boolean stillValidCache = Whitebox.getField(ClassCache.class, "validCache").getBoolean(classCache);
        final Path path = (Path) Whitebox.getField(ClassCache.class, "classCacheFile").get(classCache);
        final boolean missingClassCache = !Files.exists(path);
        Assertions.assertAll("The class cache is cleared and ready to be rebuild",
                () -> Assertions.assertTrue(stillValidCache, "Class Cache is still valid and can be rebuild"),
                () -> Assertions.assertTrue(missingClassCache, "The Class Cache file is missing"));
    }
}
