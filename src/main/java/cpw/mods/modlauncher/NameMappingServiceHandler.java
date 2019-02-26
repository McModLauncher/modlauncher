package cpw.mods.modlauncher;

import cpw.mods.modlauncher.api.*;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.*;
import java.util.stream.*;

import static cpw.mods.modlauncher.LogMarkers.*;

/**
 * Allow names to be transformed between naming domains.
 */
class NameMappingServiceHandler {
    private static final Logger LOGGER = LogManager.getLogger();
    private final ServiceLoader<INameMappingService> namingServices;
    private final Map<String, NameMappingServiceDecorator> namingLookup;

    public NameMappingServiceHandler() {
        namingServices = ServiceLoaderStreamUtils.errorHandlingServiceLoader(INameMappingService.class, serviceConfigurationError -> LOGGER.fatal("Encountered serious error loading naming service, expect problems", serviceConfigurationError));
        namingLookup = ServiceLoaderStreamUtils.toMap(namingServices, INameMappingService::mappingName, NameMappingServiceDecorator::new);
        LOGGER.debug(MODLAUNCHER,"Found naming services {}", () -> String.join(",", namingLookup.keySet()));
    }
}
