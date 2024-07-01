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

import cpw.mods.jarhandling.SecureJar;
import cpw.mods.modlauncher.api.IEnvironment;
import cpw.mods.modlauncher.api.IModuleLayerManager;
import cpw.mods.modlauncher.api.NamedPath;
import cpw.mods.modlauncher.util.ServiceLoaderUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.VisibleForTesting;
import org.objectweb.asm.*;
import org.objectweb.asm.tree.*;
import cpw.mods.modlauncher.serviceapi.ILaunchPluginService;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static cpw.mods.modlauncher.LogMarkers.*;

public class LaunchPluginHandler {
    private static final Logger LOGGER = LogManager.getLogger();
    private final Map<String, ILaunchPluginService> plugins;

    public LaunchPluginHandler(final ModuleLayerHandler layerHandler) {
        this(ServiceLoaderUtils.streamServiceLoader(()->ServiceLoader.load(layerHandler.getLayer(IModuleLayerManager.Layer.BOOT).orElseThrow(), ILaunchPluginService.class),
                e->LOGGER.fatal(MODLAUNCHER, "Encountered serious error loading launch plugin service. Things will not work well", e)));
    }

    @VisibleForTesting
    public LaunchPluginHandler(Stream<ILaunchPluginService> plugins) {
        this.plugins = plugins.collect(Collectors.toMap(ILaunchPluginService::name, Function.identity()));
        final var modlist = this.plugins.entrySet().stream().map(e->Map.of(
                "name", e.getKey(),
                "type", "PLUGINSERVICE",
                "file", ServiceLoaderUtils.fileNameFor(e.getValue().getClass())))
                .toList();
        if (Launcher.INSTANCE!=null) {
            Launcher.INSTANCE.environment().getProperty(IEnvironment.Keys.MODLIST.get())
                    .ifPresentOrElse(mods->mods.addAll(modlist),() -> {
                        throw new RuntimeException("The MODLIST isn't set, huh?");
                    });
        }
        LOGGER.debug(MODLAUNCHER,"Found launch plugins: [{}]", ()-> String.join(",", this.plugins.keySet()));
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

    public void offerScanResultsToPlugins(List<SecureJar> scanResults) {
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

    void announceLaunch(final TransformingClassLoader transformerLoader, final NamedPath[] specialPaths) {
        plugins.forEach((k, p)->p.initializeLaunch((s->transformerLoader.buildTransformedClassNodeFor(s, k)), specialPaths));
    }
}
