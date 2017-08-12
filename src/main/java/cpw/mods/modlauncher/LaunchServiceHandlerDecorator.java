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
import java.lang.reflect.InvocationTargetException;

/**
 * Decorates {@link ILaunchHandlerService} for use by the system
 */
class LaunchServiceHandlerDecorator
{
    private final ILaunchHandlerService service;

    public LaunchServiceHandlerDecorator(ILaunchHandlerService service)
    {
        this.service = service;
    }

    public void launch(String[] arguments, ClassLoader classLoader)
    {
        try
        {
            this.service.launchService(arguments, classLoader).call();
        }
        catch (Exception e)
        {
            ClassCache.invalidate(); // Make sure the class cache isn't responsible for this crash
            Throwable cause = e.getCause(); //hide invocation target exception if possible
            if (e instanceof InvocationTargetException && cause != null)
                throw new RuntimeException("Exception in launched service!", cause);
            else
                throw new RuntimeException("Failed to launch due to exception in service launcher!", e);
        }
    }

    public File[] findTransformationTargets()
    {
        return this.service.identifyTransformationTargets();
    }
}
