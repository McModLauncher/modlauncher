package cpw.mods.modlauncher;

import cpw.mods.modlauncher.api.Environment;
import cpw.mods.modlauncher.api.LauncherService;
import joptsimple.OptionParser;
import joptsimple.OptionSet;

import java.io.File;
import java.util.Map;
import java.util.ServiceLoader;
import java.util.function.BiFunction;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import static cpw.mods.modlauncher.Logging.launcherLog;
import static cpw.mods.modlauncher.ServiceLoaderUtils.parallelForEach;

/**
 * Deals with services
 */
class ServiceHandler
{
    private final ServiceLoader<LauncherService> launcherServices;
    private final Map<String, ServiceDecorator> serviceLookup;
    private final Environment environment;
    private final TransformStore transformStore;

    ServiceHandler(Environment environment, TransformStore transformStore)
    {
        launcherServices = ServiceLoader.load(LauncherService.class);
        launcherLog.info("Found services : {}", () -> ServiceLoaderUtils.toList(launcherServices));

        serviceLookup = StreamSupport.stream(launcherServices.spliterator(), false)
                .collect(Collectors.toMap(LauncherService::name, ServiceDecorator::new));
        this.environment = environment;
        this.transformStore = transformStore;
    }

    TransformingClassLoader initializeServices(ArgumentHandler argumentHandler, EnvironmentImpl environment)
    {
        launcherLog.debug("Services loading");
        // Fire onLoad to each service
        serviceLookup.values().forEach(s -> s.onLoad(environment, serviceLookup.keySet()));
        // If any reject the load, we'll enumerate them all here and drop out
        final Stream<ServiceDecorator> failedServices = serviceLookup.values().stream().filter(d -> !d.isValid());
        if (failedServices.count() > 0)
        {
            launcherLog.error("Found {} services that failed to load", failedServices::count);
            launcherLog.error("Failed services : {}", () -> failedServices.map(ServiceDecorator::getService).collect(Collectors.toList()));
            //TODO enrich exception with data from unhappy services
            throw new RuntimeException("Invalid Service found");
        }

        launcherLog.debug("Configuring option handling for services");
        // Setup option arguments for the services
        File specialJar = argumentHandler.handleArguments(environment, this::configureOptionParser, this::applyArgumentResults);

        launcherLog.debug("Services initializing");
        // Tell services to initialize themselves
        serviceLookup.values().forEach(s -> s.onInitialize(environment));

        launcherLog.debug("Services loading transformers");
        serviceLookup.values().forEach(s -> s.gatherTransformers(transformStore));

        return new TransformingClassLoader(transformStore, specialJar);
    }

    private void configureOptionParser(OptionParser parser)
    {
        parallelForEach(launcherServices,
                service -> service.arguments((a, b) -> parser.accepts(service.name() + "." + a, b))
        );
    }

    private void applyArgumentResults(OptionSet optionSet, BiFunction<String, OptionSet, LauncherService.OptionResult> resultHandler)
    {
        parallelForEach(launcherServices,
                service -> service.argumentValues(resultHandler.apply(service.name(), optionSet))
        );
    }
}
