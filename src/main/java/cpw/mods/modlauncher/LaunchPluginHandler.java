package cpw.mods.modlauncher;

import cpw.mods.modlauncher.serviceapi.*;

import java.util.*;

public class LaunchPluginHandler {

    private final Map<String, ILaunchPluginService> plugins;

    LaunchPluginHandler() {
        ServiceLoader<ILaunchPluginService> services = ServiceLoader.load(ILaunchPluginService.class);
        plugins = ServiceLoaderStreamUtils.toMap(services, ILaunchPluginService::name);
    }
    public Optional<ILaunchPluginService> get(final String name) {
        return Optional.ofNullable(plugins.get(name));
    }
}
