/*
 * Modlauncher - utility to launch Minecraft-like game environments with runtime transformation
 * Copyright ©2016-2017 cpw and others
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301  USA
 */

package cpw.mods.modlauncher;

import cpw.mods.modlauncher.api.ILaunchHandlerService;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Method;
import java.net.JarURLConnection;
import java.net.URL;
import java.util.concurrent.Callable;

/**
 * Default launch handler service - will launch minecraft
 */
public class DefaultLaunchHandlerService implements ILaunchHandlerService
{
    @Override
    public String name()
    {
        return "minecraft";
    }

    @Override
    public File[] identifyTransformationTargets()
    {
        final URL resource = getClass().getClassLoader().getResource("net/minecraft/client/main/Main.class");
        try
        {
            JarURLConnection urlConnection  = (JarURLConnection)resource.openConnection();
            return new File[] { new File(urlConnection.getJarFile().getName()) };
        }
        catch (IOException | NullPointerException e)
        {
            e.printStackTrace();
            return new File[0];
        }
    }

    @Override
    public Callable<Void> launchService(String[] arguments, ClassLoader launchClassLoader)
    {

        return () -> {
            final Class<?> mcClass = Class.forName("net.minecraft.client.main.Main", true, launchClassLoader);
            final Method mcClassMethod = mcClass.getMethod("main", String[].class);
            mcClassMethod.invoke(null, (Object)arguments);
            return null;
        };
    }
}
