package cpw.mods.modlauncher;

import cpw.mods.modlauncher.api.*;

import java.util.*;
import java.util.stream.*;

import static cpw.mods.modlauncher.Logging.*;

/**
 * Allow names to be transformed between naming domains.
 */
class NameMappingServiceHandler {
    private final ServiceLoader<INameMappingService> namingServices;
    private final Map<String, NameMappingServiceDecorator> namingLookup;

    public NameMappingServiceHandler() {
        namingServices = ServiceLoader.load(INameMappingService.class);
        launcherLog.info(MODLAUNCHER,"Found naming services {}", () -> ServiceLoaderStreamUtils.toList(namingServices));
        namingLookup = StreamSupport.stream(namingServices.spliterator(), false)
                .collect(Collectors.toMap(INameMappingService::mappingName, NameMappingServiceDecorator::new));
    }
}
