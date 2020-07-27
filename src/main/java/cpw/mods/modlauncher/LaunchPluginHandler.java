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

import cpw.mods.modlauncher.api.IEnvironment;
import cpw.mods.modlauncher.serviceapi.*;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.objectweb.asm.*;
import org.objectweb.asm.tree.*;

import javax.annotation.*;
import java.nio.file.Path;
import java.util.*;

import static cpw.mods.modlauncher.LogMarkers.*;

public class LaunchPluginHandler {
    private static final Logger LOGGER = LogManager.getLogger();
    private final Map<String, ILaunchPluginService> plugins;

    public LaunchPluginHandler() {
        ServiceLoader<ILaunchPluginService> services = ServiceLoaderStreamUtils.errorHandlingServiceLoader(ILaunchPluginService.class,
                e->LOGGER.fatal(MODLAUNCHER, "Encountered serious error loading launch plugin service. Things will not work well", e));
        plugins = ServiceLoaderStreamUtils.toMap(services, ILaunchPluginService::name);
        final List<Map<String, String>> modlist = new ArrayList<>();
        plugins.forEach((name, plugin)->{
            HashMap<String,String> mod = new HashMap<>();
            mod.put("name", name);
            mod.put("type", "PLUGINSERVICE");
            String fName = plugin.getClass().getProtectionDomain().getCodeSource().getLocation().getFile();
            mod.put("file", fName.substring(fName.lastIndexOf("/")));
            modlist.add(mod);
        });
        if (Launcher.INSTANCE!=null) {
            final List<Map<String, String>> mods = Launcher.INSTANCE.environment().getProperty(IEnvironment.Keys.MODLIST.get()).orElseThrow(() -> new RuntimeException("The MODLIST isn't set, huh?"));
            mods.addAll(modlist);
        }
        LOGGER.debug(MODLAUNCHER,"Found launch plugins: [{}]", ()-> String.join(",", plugins.keySet()));
    }

    public Optional<ILaunchPluginService> get(final String name) {
        return Optional.ofNullable(plugins.get(name));
    }

    public EnumMap<ILaunchPluginService.Phase, List<ILaunchPluginService>> computeLaunchPluginTransformerSet(final Type className, final boolean isEmpty, final String reason, final TransformerAuditTrail auditTrail) {
        Set<ILaunchPluginService> uniqueValues = new HashSet<>();
        final EnumMap<ILaunchPluginService.Phase, List<ILaunchPluginService>> phaseObjectEnumMap = new EnumMap<>(ILaunchPluginService.Phase.class);
        for (ILaunchPluginService plugin : plugins.values()) {
            for (ILaunchPluginService.Phase ph : plugin.handlesClass(className, isEmpty, reason)) {
                phaseObjectEnumMap.computeIfAbsent(ph, e -> new ArrayList<>()).add(plugin);
                if (uniqueValues.add(plugin)) {
                    plugin.customAuditConsumer(className.getClassName(), strings -> auditTrail.addPluginCustomAuditTrail(className.getClassName(), plugin, strings));
                }
            }
        }
        LOGGER.debug(LAUNCHPLUGIN, "LaunchPluginService {}", ()->phaseObjectEnumMap);
        return phaseObjectEnumMap;
    }

    void offerScanResultsToPlugins(List<Map.Entry<String, Path>> scanResults) {
        plugins.forEach((n,p)->p.addResources(scanResults));
    }

    int offerClassNodeToPlugins(final ILaunchPluginService.Phase phase, final List<ILaunchPluginService> plugins, @Nullable final ClassNode node, final Type className, TransformerAuditTrail auditTrail, final String reason) {
        int flags = 0;
        for (ILaunchPluginService iLaunchPluginService : plugins) {
            LOGGER.debug(LAUNCHPLUGIN, "LauncherPluginService {} offering transform {}", iLaunchPluginService.name(), className.getClassName());
            final int pluginFlags = iLaunchPluginService.processClassWithFlags(phase, node, className, reason);
            if (pluginFlags != ILaunchPluginService.ComputeFlags.NO_REWRITE) {
                auditTrail.addPluginAuditTrail(className.getClassName(), iLaunchPluginService, phase);
                LOGGER.debug(LAUNCHPLUGIN, "LauncherPluginService {} transformed {} with class compute flags {}", iLaunchPluginService.name(), className.getClassName(), pluginFlags);
                flags |= pluginFlags;
            }
        }
        LOGGER.debug(LAUNCHPLUGIN, "Final flags state for {} is {}", className.getClassName(), flags);
        return flags;
    }

    void announceLaunch(final TransformingClassLoader transformerLoader, final Path[] specialPaths) {
        plugins.forEach((k, p)->p.initializeLaunch((s->transformerLoader.buildTransformedClassNodeFor(s, k)), specialPaths));
    }
}
