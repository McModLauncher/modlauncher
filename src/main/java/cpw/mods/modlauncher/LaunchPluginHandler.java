package cpw.mods.modlauncher;

import cpw.mods.modlauncher.serviceapi.*;

import java.util.*;
import java.util.stream.*;

import static cpw.mods.modlauncher.Logging.*;

public class LaunchPluginHandler {

    private final Map<String, ILaunchPluginService> plugins;

    LaunchPluginHandler() {
        ServiceLoader<ILaunchPluginService> services = ServiceLoader.load(ILaunchPluginService.class);
        plugins = ServiceLoaderStreamUtils.toMap(services, ILaunchPluginService::name);
        launcherLog.info("Found launch plugins: [{}]", ()-> plugins.keySet().stream().collect(Collectors.joining()));
    }
    public Optional<ILaunchPluginService> get(final String name) {
        return Optional.ofNullable(plugins.get(name));
    }
}
