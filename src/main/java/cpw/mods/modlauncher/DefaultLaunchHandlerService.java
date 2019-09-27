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

import java.lang.reflect.*;
import java.nio.file.*;
import java.util.concurrent.*;

/**
 * Default launch handler service - will launch minecraft
 */
public class DefaultLaunchHandlerService implements ILaunchHandlerService {
    public static final String LAUNCH_PROPERTY = "minecraft.client.jar";
    public static final String LAUNCH_PATH_STRING = System.getProperty(LAUNCH_PROPERTY);

    @Override
    public String name() {
        return "minecraft";
    }

    @Override
    public void configureTransformationClassLoader(final ITransformingClassLoaderBuilder builder) {
        if (LAUNCH_PATH_STRING == null) {
            throw new IllegalStateException("Missing "+ LAUNCH_PROPERTY +" environment property. Update your launcher!");
        }
        builder.addTransformationPath(FileSystems.getDefault().getPath(LAUNCH_PATH_STRING));
    }

    @Override
    public Callable<Void> launchService(String[] arguments, ITransformingClassLoader launchClassLoader) {

        return () -> {
            final Class<?> mcClass = Class.forName("net.minecraft.client.main.Main", true, launchClassLoader.getInstance());
            final Method mcClassMethod = mcClass.getMethod("main", String[].class);
            mcClassMethod.invoke(null, (Object) arguments);
            return null;
        };
    }

    @Override
    public Path[] getPaths() {
        return new Path[] {FileSystems.getDefault().getPath(LAUNCH_PATH_STRING)};
    }
}
