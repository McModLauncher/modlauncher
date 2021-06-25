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

import java.nio.file.*;

/**
 * Decorates {@link ILaunchHandlerService} for use by the system
 */
class LaunchServiceHandlerDecorator {
    private final ILaunchHandlerService service;

    public LaunchServiceHandlerDecorator(ILaunchHandlerService service) {
        this.service = service;
    }

    public void launch(String[] arguments, ModuleLayer gameLayer) {
        try {
            this.service.launchService(arguments, gameLayer).call();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public void configureTransformationClassLoaderBuilder(ITransformingClassLoaderBuilder builder) {
        this.service.configureTransformationClassLoader(builder);
    }

    ILaunchHandlerService getService() {
        return service;
    }
}
