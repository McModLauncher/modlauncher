package cpw.mods.modlauncher;

import cpw.mods.modlauncher.serviceapi.*;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.objectweb.asm.*;
import org.objectweb.asm.tree.*;

import javax.annotation.*;
import java.util.*;
import java.util.stream.*;

import static cpw.mods.modlauncher.LogMarkers.*;

public class LaunchPluginHandler {
    private static final Logger LOGGER = LogManager.getLogger();
    private final Map<String, ILaunchPluginService> plugins;

    public LaunchPluginHandler() {
        ServiceLoader<ILaunchPluginService> services = ServiceLoader.load(ILaunchPluginService.class);
        plugins = ServiceLoaderStreamUtils.toMap(services, ILaunchPluginService::name);
        LOGGER.debug(MODLAUNCHER,"Found launch plugins: [{}]", ()-> String.join(",", plugins.keySet()));
    }
    public Optional<ILaunchPluginService> get(final String name) {
        return Optional.ofNullable(plugins.get(name));
    }

    public EnumMap<ILaunchPluginService.Phase, List<ILaunchPluginService>> computeLaunchPluginTransformerSet(final Type className, final boolean isEmpty) {
        final EnumMap<ILaunchPluginService.Phase, List<ILaunchPluginService>> out = plugins.values().stream()
                .collect(
                        () -> new EnumMap<>(ILaunchPluginService.Phase.class),
                        (map, plugin) -> {
                            plugin.handlesClass(className, isEmpty)
                                    .forEach(phase -> map.computeIfAbsent(phase, ph->new ArrayList<>()).add(plugin));
                        },
                        EnumMap::putAll
                );

        LOGGER.debug(LAUNCHPLUGIN, "LaunchPluginService {}", () -> out);
        return out;
    }

    public boolean offerClassNodeToPlugins(final ILaunchPluginService.Phase phase, final List<ILaunchPluginService> plugins, @Nullable final ClassNode node, final Type className) {
        return plugins.stream().
                peek(iLaunchPluginService -> LOGGER.debug(LAUNCHPLUGIN, "LauncherPluginService {} transforming {}", iLaunchPluginService.name(), className)).
                map(iLaunchPluginService -> iLaunchPluginService.processClass(phase, node, className)).
                reduce(Boolean.FALSE, Boolean::logicalOr);
    }
}
