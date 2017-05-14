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

class ServicesHandler
{
    private final ServiceLoader<LauncherService> launcherServices;
    private final Map<String, LauncherServiceMetadataDecorator> serviceLookup;
    private final Environment environment;
    private final TransformStore transformStore;

    ServicesHandler(Environment environment, TransformStore transformStore)
    {
        launcherServices = ServiceLoader.load(LauncherService.class);
        launcherLog.info("Found services : {}", () -> ServiceLoaderUtils.toList(launcherServices));

        serviceLookup = StreamSupport.stream(launcherServices.spliterator(), false)
                .collect(Collectors.toMap(LauncherService::name, LauncherServiceMetadataDecorator::new));

        this.environment = environment;
        this.transformStore = transformStore;
    }

    TransformingClassLoader initializeServicesAndConstructClassLoader(ArgumentHandler argumentHandler, EnvironmentImpl environment)
    {
        loadAllServices(environment);
        throwIfServicesFailedToLoad();

        File specialJar = configureArgumentsForServicesAndFindMinecraftJar(argumentHandler, environment);

        initialiseAllServices(environment);
        initialiseAllServiceTransformers();

        return new TransformingClassLoader(transformStore, specialJar);
    }

    private File configureArgumentsForServicesAndFindMinecraftJar(ArgumentHandler argumentHandler, EnvironmentImpl environment)
    {
        launcherLog.debug("Configuring option handling for services");

        return argumentHandler.handleArgumentsAndFindMinecraftJar(environment, this::offerArgumentsToAllServices, this::applyArgumentsToAllServices);
    }

    private void offerArgumentsToAllServices(OptionParser parser)
    {
        parallelForEach(launcherServices,
                service -> service.arguments((a, b) -> parser.accepts(service.name() + "." + a, b))
        );
    }

    private void applyArgumentsToAllServices(OptionSet optionSet, BiFunction<String, OptionSet, LauncherService.OptionResult> resultHandler)
    {
        parallelForEach(launcherServices,
                service -> service.argumentValues(resultHandler.apply(service.name(), optionSet))
        );
    }

    private void initialiseAllServiceTransformers()
    {
        launcherLog.debug("Services loading transformers");

        serviceLookup.values().forEach(s -> s.gatherTransformers(transformStore));
    }

    private void initialiseAllServices(EnvironmentImpl environment)
    {
        launcherLog.debug("Services initializing");

        serviceLookup.values().forEach(s -> s.onInitialize(environment));
    }

    private void throwIfServicesFailedToLoad() throws RuntimeException
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

    private void loadAllServices(EnvironmentImpl environment)
    {
        launcherLog.debug("Services loading");

        serviceLookup.values().forEach(s -> s.onLoad(environment, serviceLookup.keySet()));
    }
}
