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
import org.apache.logging.log4j.util.Strings;

import java.io.File;
import java.util.Arrays;
import java.util.Map;
import java.util.ServiceLoader;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import static cpw.mods.modlauncher.Logging.launcherLog;

/**
 * Identifies the launch target and dispatches to it
 */
class LaunchServiceHandler
{
    private final ServiceLoader<ILaunchHandlerService> launchHandlerServices;
    private final Map<String, LaunchServiceHandlerDecorator> launchHandlerLookup;

    public LaunchServiceHandler()
    {
        launchHandlerServices = ServiceLoader.load(ILaunchHandlerService.class);
        launcherLog.info("Found launch services {}", () -> ServiceLoaderStreamUtils.toList(launchHandlerServices));
        launchHandlerLookup = StreamSupport.stream(launchHandlerServices.spliterator(), false)
                .collect(Collectors.toMap(ILaunchHandlerService::name, LaunchServiceHandlerDecorator::new));
    }

    private void launch(String target, String[] arguments, ClassLoader classLoader) {
        launchHandlerLookup.get(target).launch(arguments, classLoader);
    }

    public void launch(ArgumentHandler argumentHandler, TransformingClassLoader classLoader)
    {
        String launchTarget = argumentHandler.getLaunchTarget();
        String[] args = argumentHandler.buildArgumentList();
        launch(launchTarget, args, classLoader);
    }

    public File[] identifyTransformationTargets(ArgumentHandler argumentHandler)
    {
        final String launchTarget = argumentHandler.getLaunchTarget();
        if (Strings.isBlank(launchTarget))
            throw new RuntimeException("Cannot launch because no launch target could be found!");
        final LaunchServiceHandlerDecorator serviceHandlerDecorator = launchHandlerLookup.get(launchTarget);
        if (serviceHandlerDecorator == null)
            throw new RuntimeException("Cannot launch because the launch target " + launchTarget + " could not be found!");
        final File[] transformationTargets = serviceHandlerDecorator.findTransformationTargets();
        final File[] specialJar = argumentHandler.getSpecialJars();
        return Stream.concat(Arrays.stream(transformationTargets), Arrays.stream(specialJar)).collect(Collectors.toList()).toArray(new File[0]);
    }
}
