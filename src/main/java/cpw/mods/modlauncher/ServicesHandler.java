package cpw.mods.modlauncher;

import cpw.mods.modlauncher.api.IEnvironment;
import cpw.mods.modlauncher.api.ILauncherService;
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
import static cpw.mods.modlauncher.ServiceLoaderStreamUtils.parallelForEach;

class ServicesHandler
{
    private final ServiceLoader<ILauncherService> launcherServices;
    private final Map<String, LauncherServiceMetadataDecorator> serviceLookup;
    private final IEnvironment environment;
    private final TransformStore transformStore;

    ServicesHandler(IEnvironment environment, TransformStore transformStore)
    {
        launcherServices = ServiceLoader.load(ILauncherService.class);
        launcherLog.info("Found services : {}", () -> ServiceLoaderStreamUtils.toList(launcherServices));

        serviceLookup = StreamSupport.stream(launcherServices.spliterator(), false)
                .collect(Collectors.toMap(ILauncherService::name, LauncherServiceMetadataDecorator::new));

        this.environment = environment;
        this.transformStore = transformStore;
    }

    TransformingClassLoader initializeServices(ArgumentHandler argumentHandler, Environment environment)
    {
        loadServices(environment);
        throwServiceRejectedEnvironment();

        File specialJar = processArguments(argumentHandler, environment);

        initialiseServices(environment);
        initialiseServiceTransformers();

        return new TransformingClassLoader(transformStore, specialJar);
    }

    private File processArguments(ArgumentHandler argumentHandler, Environment environment)
    {
        launcherLog.debug("Configuring option handling for services");

        return argumentHandler.processArguments(environment, this::computeArgumentsForServices, this::offerArgumentResultsToServices);
    }

    private void computeArgumentsForServices(OptionParser parser)
    {
        parallelForEach(launcherServices,
                service -> service.arguments((a, b) -> parser.accepts(service.name() + "." + a, b))
        );
    }

    private void offerArgumentResultsToServices(OptionSet optionSet, BiFunction<String, OptionSet, ILauncherService.OptionResult> resultHandler)
    {
        parallelForEach(launcherServices,
                service -> service.argumentValues(resultHandler.apply(service.name(), optionSet))
        );
    }

    private void initialiseServiceTransformers()
    {
        launcherLog.debug("Services loading transformers");

        serviceLookup.values().forEach(s -> s.gatherTransformers(transformStore));
    }

    private void initialiseServices(Environment environment)
    {
        launcherLog.debug("Services initializing");

        serviceLookup.values().forEach(s -> s.onInitialize(environment));
    }

    private void throwServiceRejectedEnvironment() throws RuntimeException
    {
        final Stream<LauncherServiceMetadataDecorator> failedServices = serviceLookup.values().stream().filter(d -> !d.isValid());
        if (failedServices.count() > 0)
        {
            launcherLog.error("Found {} services that failed to load", failedServices::count);
            launcherLog.error("Failed services : {}", () -> failedServices.map(LauncherServiceMetadataDecorator::getService).collect(Collectors.toList()));
            //TODO enrich exception with data from unhappy services
            throw new RuntimeException("Invalid Service found");
        }
    }

    private void loadServices(Environment environment)
    {
        launcherLog.debug("Services loading");

        serviceLookup.values().forEach(s -> s.onLoad(environment, serviceLookup.keySet()));
    }
}
