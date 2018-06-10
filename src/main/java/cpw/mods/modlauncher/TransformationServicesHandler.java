package cpw.mods.modlauncher;

import cpw.mods.modlauncher.api.*;
import joptsimple.*;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.nio.file.*;
import java.util.*;
import java.util.function.*;
import java.util.stream.*;

import static cpw.mods.modlauncher.Logging.*;
import static cpw.mods.modlauncher.ServiceLoaderStreamUtils.*;

class TransformationServicesHandler {
    private static final Logger LOGGER = LogManager.getLogger("Launcher");
    private final ServiceLoader<ITransformationService> transformationServices;
    private final Map<String, TransformationServiceDecorator> serviceLookup;
    private final TransformStore transformStore;

    TransformationServicesHandler(TransformStore transformStore) {
        transformationServices = ServiceLoader.load(ITransformationService.class);
        LOGGER.info(MODLAUNCHER,"Found transformer services : [{}]", () ->
                ServiceLoaderStreamUtils.toList(transformationServices).stream().
                        map(ITransformationService::name).collect(Collectors.joining()));

        serviceLookup = StreamSupport.stream(transformationServices.spliterator(), false)
                .collect(Collectors.toMap(ITransformationService::name, TransformationServiceDecorator::new));

        this.transformStore = transformStore;
    }

    void initializeTransformationServices(ArgumentHandler argumentHandler, Environment environment) {
        loadTransformationServices(environment);
        validateTransformationServices();

        processArguments(argumentHandler, environment);

        initialiseTransformationServices(environment);
        initialiseServiceTransformers();
    }

    TransformingClassLoader buildTransformingClassLoader(final LaunchPluginHandler pluginHandler, Path... specialJars) {
        return new TransformingClassLoader(transformStore, pluginHandler, specialJars);
    }

    private void processArguments(ArgumentHandler argumentHandler, Environment environment) {
        LOGGER.debug(MODLAUNCHER,"Configuring option handling for services");

        argumentHandler.processArguments(environment, this::computeArgumentsForServices, this::offerArgumentResultsToServices);
    }

    private void computeArgumentsForServices(OptionParser parser) {
        parallelForEach(transformationServices,
                service -> service.arguments((a, b) -> parser.accepts(service.name() + "." + a, b))
        );
    }

    private void offerArgumentResultsToServices(OptionSet optionSet, BiFunction<String, OptionSet, ITransformationService.OptionResult> resultHandler) {
        parallelForEach(transformationServices,
                service -> service.argumentValues(resultHandler.apply(service.name(), optionSet))
        );
    }

    private void initialiseServiceTransformers() {
        LOGGER.debug(MODLAUNCHER,"Transformation services loading transformers");

        serviceLookup.values().forEach(s -> s.gatherTransformers(transformStore));
    }

    private void initialiseTransformationServices(Environment environment) {
        LOGGER.debug(MODLAUNCHER,"Transformation services initializing");

        serviceLookup.values().forEach(s -> s.onInitialize(environment));
    }

    private void validateTransformationServices() throws RuntimeException {
        if (serviceLookup.values().stream().filter(d -> !d.isValid()).count() > 0) {
            LOGGER.error(MODLAUNCHER,"Found {} services that failed to load", serviceLookup.values().stream().filter(d -> !d.isValid())::count);
            LOGGER.error(MODLAUNCHER,"Failed services : {}", () -> serviceLookup.values().stream().filter(d -> !d.isValid()).map(TransformationServiceDecorator::getService).collect(Collectors.toList()));
            //TODO enrich exception with data from unhappy services
            throw new RuntimeException("Invalid Service found");
        }
    }

    private void loadTransformationServices(Environment environment) {
        LOGGER.debug(MODLAUNCHER,"Transformation services loading");

        serviceLookup.values().forEach(s -> s.onLoad(environment, serviceLookup.keySet()));
    }
}
