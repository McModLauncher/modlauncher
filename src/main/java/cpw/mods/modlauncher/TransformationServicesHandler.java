package cpw.mods.modlauncher;

import cpw.mods.modlauncher.api.*;
import joptsimple.*;

import java.nio.file.*;
import java.util.*;
import java.util.function.*;
import java.util.stream.*;

import static cpw.mods.modlauncher.Logging.*;
import static cpw.mods.modlauncher.ServiceLoaderStreamUtils.*;

class TransformationServicesHandler {
    private final ServiceLoader<ITransformationService> transformationServices;
    private final Map<String, TransformationServiceDecorator> serviceLookup;
    private final TransformStore transformStore;

    TransformationServicesHandler(TransformStore transformStore) {
        transformationServices = ServiceLoader.load(ITransformationService.class);
        launcherLog.info("Found transformer services : [{}]", () ->
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
        launcherLog.debug("Configuring option handling for services");

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
        launcherLog.debug("Transformation services loading transformers");

        serviceLookup.values().forEach(s -> s.gatherTransformers(transformStore));
    }

    private void initialiseTransformationServices(Environment environment) {
        launcherLog.debug("Transformation services initializing");

        serviceLookup.values().forEach(s -> s.onInitialize(environment));
    }

    private void validateTransformationServices() throws RuntimeException {
        final Stream<TransformationServiceDecorator> failedServices = serviceLookup.values().stream().filter(d -> !d.isValid());
        if (failedServices.count() > 0) {
            launcherLog.error("Found {} services that failed to load", failedServices::count);
            launcherLog.error("Failed services : {}", () -> failedServices.map(TransformationServiceDecorator::getService).collect(Collectors.toList()));
            //TODO enrich exception with data from unhappy services
            throw new RuntimeException("Invalid Service found");
        }
    }

    private void loadTransformationServices(Environment environment) {
        launcherLog.debug("Transformation services loading");

        serviceLookup.values().forEach(s -> s.onLoad(environment, serviceLookup.keySet()));
    }
}
