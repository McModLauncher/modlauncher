package cpw.mods.modlauncher;

import cpw.mods.modlauncher.api.INameMappingService;

import java.util.Map;
import java.util.ServiceLoader;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import static cpw.mods.modlauncher.Logging.launcherLog;

/**
 * Allow names to be transformed between naming domains.
 */
public class NameMappingServiceHandler
{
    private final ServiceLoader<INameMappingService> namingServices;
    private final Map<String, NameMappingServiceDecorator> namingLookup;

    public NameMappingServiceHandler()
    {
        namingServices = ServiceLoader.load(INameMappingService.class);
        launcherLog.info("Found naming services {}", () -> ServiceLoaderStreamUtils.toList(namingServices));
        namingLookup = StreamSupport.stream(namingServices.spliterator(), false)
                .collect(Collectors.toMap(INameMappingService::mappingName, NameMappingServiceDecorator::new));
    }
}
