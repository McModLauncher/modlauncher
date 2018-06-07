package cpw.mods.modlauncher;

import cpw.mods.modlauncher.api.*;

import java.lang.invoke.*;
import java.lang.reflect.*;
import java.nio.file.*;
import java.util.Arrays;
import java.util.concurrent.*;
import java.util.function.*;

/**
 * Test harness launch service - this will do nothing, but will take "test.harness" and offer it to the transformer
 * system. Should be ideal for testing external transformers.
 */
public class TestingLaunchHandlerService implements ILaunchHandlerService {
    @Override
    public String name() {
        return "testharness";
    }

    @Override
    public Path[] identifyTransformationTargets() {
        return Arrays.stream(System.getProperty("test.harness").split(",")).
                map(FileSystems.getDefault()::getPath).
                toArray(Path[]::new);
    }

    @Override
    public Callable<Void> launchService(String[] arguments, ITransformingClassLoader launchClassLoader) {
        try {
            Class<?> callableLaunch = Class.forName(System.getProperty("test.harness.callable"));
            final MethodHandles.Lookup lookup = MethodHandles.lookup();
            final CallSite site = LambdaMetafactory.metafactory(lookup,
                    "get",
                    MethodType.methodType(Supplier.class),
                    MethodType.methodType(Object.class),
                    lookup.findStatic(callableLaunch, "supplier", MethodType.methodType(Callable.class)),
                    MethodType.methodType(Supplier.class));
            final Supplier<Callable<Void>> supplier = (Supplier<Callable<Void>>) site.getTarget().invoke();
            return supplier.get();
        } catch (ClassNotFoundException | NoSuchMethodException | LambdaConversionException | IllegalAccessException | InstantiationException e) {
            throw new RuntimeException(e);
        } catch (Throwable throwable) {
            throwable.printStackTrace();
        }
        return ()-> null;
    }
}
