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
import cpw.mods.modlauncher.serviceapi.*;

import java.util.*;
import java.util.function.BiFunction;
import java.util.function.Function;

/**
 * Environment implementation class
 */
public final class Environment implements IEnvironment {
    private final TypesafeMap environment;
    private final Launcher launcher;

    Environment(Launcher launcher) {
        environment = new TypesafeMap(IEnvironment.class);
        this.launcher = launcher;
    }

    @Override
    public final <T> Optional<T> getProperty(TypesafeMap.Key<T> key) {
        return environment.get(key);
    }

    @Override
    public Optional<ILaunchPluginService> findLaunchPlugin(final String name) {
        return launcher.findLaunchPlugin(name);
    }

    @Override
    public Optional<ILaunchHandlerService> findLaunchHandler(final String name) {
        return launcher.findLaunchHandler(name);
    }

    @Override
    public Optional<BiFunction<INameMappingService.Domain, String, String>> findNameMapping(final String targetMapping) {
        return launcher.findNameMapping(targetMapping);
    }

    @Override
    public <T> T computePropertyIfAbsent(final TypesafeMap.Key<T> key, final Function<? super TypesafeMap.Key<T>, ? extends T> valueFunction) {
        return environment.computeIfAbsent(key, valueFunction);
    }
}
