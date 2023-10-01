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
import cpw.mods.modlauncher.util.ServiceLoaderUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.*;
import java.util.stream.*;

import static cpw.mods.modlauncher.LogMarkers.*;

/**
 * Identifies the launch target and dispatches to it
 */
class LaunchServiceHandler {
    private static final Logger LOGGER = LogManager.getLogger();
    private final Map<String, LaunchServiceHandlerDecorator> launchHandlerLookup;

    public LaunchServiceHandler(final ModuleLayerHandler layerHandler) {
        this.launchHandlerLookup = ServiceLoaderUtils.streamServiceLoader(()->ServiceLoader.load(layerHandler.getLayer(IModuleLayerManager.Layer.BOOT).orElseThrow(), ILaunchHandlerService.class), sce -> LOGGER.fatal("Encountered serious error loading transformation service, expect problems", sce))
                .collect(Collectors.toMap(ILaunchHandlerService::name, LaunchServiceHandlerDecorator::new));
        LOGGER.debug(MODLAUNCHER,"Found launch services [{}]", () -> String.join(",",launchHandlerLookup.keySet()));
    }

    public Optional<ILaunchHandlerService> findLaunchHandler(final String name) {
        return Optional.ofNullable(launchHandlerLookup.getOrDefault(name, null)).map(LaunchServiceHandlerDecorator::service);
    }

    private void launch(String target, String[] arguments, ModuleLayer gameLayer, TransformingClassLoader classLoader, final LaunchPluginHandler launchPluginHandler) {
        final LaunchServiceHandlerDecorator launchServiceHandlerDecorator = launchHandlerLookup.get(target);
        final NamedPath[] paths = launchServiceHandlerDecorator.service().getPaths();
        launchPluginHandler.announceLaunch(classLoader, paths);
        LOGGER.info(MODLAUNCHER, "Launching target '{}' with arguments {}", target, hideAccessToken(arguments));
        launchServiceHandlerDecorator.launch(arguments, gameLayer);
    }

    static List<String> hideAccessToken(String[] arguments) {
        final ArrayList<String> output = new ArrayList<>();
        for (int i = 0; i < arguments.length; i++) {
            if (i > 0 && Objects.equals(arguments[i-1], "--accessToken")) {
                output.add("❄❄❄❄❄❄❄❄");
            } else {
                output.add(arguments[i]);
            }
        }
        return output;
    }

    public void launch(ArgumentHandler argumentHandler, ModuleLayer gameLayer, TransformingClassLoader classLoader, final LaunchPluginHandler launchPluginHandler) {
        String launchTarget = argumentHandler.getLaunchTarget();
        String[] args = argumentHandler.buildArgumentList();
        launch(launchTarget, args, gameLayer, classLoader, launchPluginHandler);
    }

    void validateLaunchTarget(final ArgumentHandler argumentHandler) {
        if (!launchHandlerLookup.containsKey(argumentHandler.getLaunchTarget())) {
            LOGGER.error(MODLAUNCHER, "Cannot find launch target {}, unable to launch",
                    argumentHandler.getLaunchTarget());
            throw new RuntimeException("Cannot find launch target");
        }
    }
}
