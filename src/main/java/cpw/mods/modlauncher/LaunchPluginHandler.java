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

    public boolean offerClassNodeToPlugins(final ILaunchPluginService.Phase phase, final List<ILaunchPluginService> plugins, @Nullable final ClassNode node, final Type className, TransformerAuditTrail auditTrail) {
        boolean needsWrite = false;
        for (ILaunchPluginService iLaunchPluginService : plugins) {
            LOGGER.debug(LAUNCHPLUGIN, "LauncherPluginService {} transforming {}", iLaunchPluginService.name(), className);
            if (iLaunchPluginService.processClass(phase, node, className)) {
                auditTrail.addPluginAuditTrail(className.getClassName(), iLaunchPluginService, phase);
                needsWrite = true;
            }
        }
        return needsWrite;
    }
}
