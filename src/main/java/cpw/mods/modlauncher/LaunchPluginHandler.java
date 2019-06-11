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

import cpw.mods.modlauncher.serviceapi.*;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.objectweb.asm.*;
import org.objectweb.asm.tree.*;

import javax.annotation.*;
import java.util.*;
import java.util.stream.Collectors;

import static cpw.mods.modlauncher.LogMarkers.*;

public class LaunchPluginHandler {
    private static final Logger LOGGER = LogManager.getLogger();
    private final Map<String, ILaunchPluginService> plugins;

    public LaunchPluginHandler() {
        ServiceLoader<ILaunchPluginService> services = ServiceLoaderStreamUtils.errorHandlingServiceLoader(ILaunchPluginService.class,
                e->LOGGER.fatal(MODLAUNCHER, "Encountered serious error loading launch plugin service. Things will not work well", e));
        plugins = ServiceLoaderStreamUtils.toMap(services, ILaunchPluginService::name);
        LOGGER.debug(MODLAUNCHER,"Found launch plugins: [{}]", ()-> String.join(",", plugins.keySet()));
    }

    public Optional<ILaunchPluginService> get(final String name) {
        return Optional.ofNullable(plugins.get(name));
    }

    public EnumMap<ILaunchPluginService.Phase, List<ILaunchPluginService>> computeLaunchPluginTransformerSet(final Type className, final boolean isEmpty) {
        final EnumMap<ILaunchPluginService.Phase, List<ILaunchPluginService>> phaseObjectEnumMap = new EnumMap<>(ILaunchPluginService.Phase.class);
        plugins.forEach((n,pl)-> pl.handlesClass(className, isEmpty).forEach(ph->phaseObjectEnumMap.computeIfAbsent(ph, e->new ArrayList<>()).add(pl)));
        LOGGER.debug(LAUNCHPLUGIN, "LaunchPluginService {}", ()->phaseObjectEnumMap);
        return phaseObjectEnumMap;
    }

    boolean offerClassNodeToPlugins(final ILaunchPluginService.Phase phase, final List<ILaunchPluginService> plugins, @Nullable final ClassNode node, final Type className, TransformerAuditTrail auditTrail) {
        boolean needsRewriting = false;
        for (ILaunchPluginService iLaunchPluginService : plugins) {
            LOGGER.debug(LAUNCHPLUGIN, "LauncherPluginService {} offering transform {}", iLaunchPluginService.name(), className.getClassName());
            if (iLaunchPluginService.processClass(phase, node, className)) {
                auditTrail.addPluginAuditTrail(className.getClassName(), iLaunchPluginService, phase);
                LOGGER.debug(LAUNCHPLUGIN, "LauncherPluginService {} transformed {}", iLaunchPluginService.name(), className.getClassName());
                needsRewriting = true;
            }
        }
        return needsRewriting;
    }
}
