/*
 * ModLauncher - for launching Java programs with in-flight transformation ability.
 *
 *     Copyright (C) 2017-2019 cpw
 *
 *     This program is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU Lesser General Public License as published by
 *     the Free Software Foundation, version 3 of the License.
 *
 *     This program is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU Lesser General Public License for more details.
 *
 *     You should have received a copy of the GNU Lesser General Public License
 *     along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package cpw.mods.modlauncher;

import cpw.mods.modlauncher.api.*;

import java.lang.invoke.*;
import java.nio.file.*;
import java.util.Arrays;
import java.util.concurrent.*;

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
    public void configureTransformationClassLoader(final ITransformingClassLoaderBuilder builder) {
        Arrays.stream(System.getProperty("test.harness").split(",")).
                map(FileSystems.getDefault()::getPath).
                forEach(builder::addTransformationPath);
    }

    public Callable<Void> launchService(String[] arguments, ModuleLayer gameLayer) {
        try {
            Class<?> callableLaunch = Class.forName(System.getProperty("test.harness.callable"));
            MethodHandle handle = MethodHandles.lookup().findStatic(callableLaunch, "supplier", MethodType.methodType(Callable.class));
            return (Callable<Void>) handle.invoke();
        } catch (ClassNotFoundException | NoSuchMethodException | LambdaConversionException | IllegalAccessException | InstantiationException e) {
            throw new RuntimeException(e);
        } catch (Throwable throwable) {
            throwable.printStackTrace();
        }
        return () -> null;
    }
}
