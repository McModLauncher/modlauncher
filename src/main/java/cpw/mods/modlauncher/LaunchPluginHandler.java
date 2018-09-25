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
        LOGGER.info(MODLAUNCHER,"Found launch plugins: [{}]", ()-> String.join(",", plugins.keySet()));
    }
    public Optional<ILaunchPluginService> get(final String name) {
        return Optional.ofNullable(plugins.get(name));
    }

    public List<String> getPluginsTransforming(final Type className, final boolean isEmpty) {
        return plugins.entrySet().stream().filter(p -> p.getValue().handlesClass(className, isEmpty)).
                peek(e-> LOGGER.debug(LAUNCHPLUGIN,"LaunchPluginService {} wants to handle {}", e.getKey(), className)).
                collect(ArrayList<String>::new, (l,e) -> l.add(e.getKey()), ArrayList::addAll);
    }

    public ClassNode offerClassNodeToPlugins(final List<String> pluginNames, @Nullable final ClassNode node, final Type className) {
        ClassNode intermediate = node;
        for (String plugin: pluginNames) {
            final ILaunchPluginService iLaunchPluginService = plugins.get(plugin);
            LOGGER.debug(LAUNCHPLUGIN,"LauncherPluginService {} transforming {}", plugin, className);
            intermediate = iLaunchPluginService.processClass(intermediate, className);
        }
        return intermediate;
    }
}
