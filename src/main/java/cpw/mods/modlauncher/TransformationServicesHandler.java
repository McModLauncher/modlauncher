/*
 * ModLauncher - for launching Java programs with in-flight transformation ability.
 *
 *     Copyright (C) 2017-2019 cpw
 *
 *     This program is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU Lesser General Public License as published by
 *     the Free Software Foundation, version 3 of the License.
 *
 *     This program is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU Lesser General Public License for more details.
 *
 *     You should have received a copy of the GNU Lesser General Public License
 *     along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package cpw.mods.modlauncher;

import cpw.mods.jarhandling.SecureJar;
import cpw.mods.modlauncher.api.*;
import cpw.mods.modlauncher.serviceapi.ITransformerDiscoveryService;
import cpw.mods.modlauncher.util.ServiceLoaderUtils;
import joptsimple.*;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;


import java.net.URL;
import java.util.*;
import java.util.function.*;
import java.util.stream.*;

import static cpw.mods.modlauncher.LogMarkers.*;

class TransformationServicesHandler {
    private static final Logger LOGGER = LogManager.getLogger();
    private Map<String, TransformationServiceDecorator> serviceLookup;
    private final TransformStore transformStore;
    private final ModuleLayerHandler layerHandler;

    TransformationServicesHandler(TransformStore transformStore, ModuleLayerHandler layerHandler) {
        this.transformStore = transformStore;
        this.layerHandler = layerHandler;
    }

    List<ITransformationService.Resource> initializeTransformationServices(ArgumentHandler argumentHandler, Environment environment, final NameMappingServiceHandler nameMappingServiceHandler) {
        loadTransformationServices(environment);
        validateTransformationServices();
        processArguments(argumentHandler, environment);
        initialiseTransformationServices(environment);
        // force the naming to "mojang" if nothing has been populated during transformer setup
        environment.computePropertyIfAbsent(IEnvironment.Keys.NAMING.get(), a-> "mojang");
        nameMappingServiceHandler.bindNamingServices(environment.getProperty(Environment.Keys.NAMING.get()).orElse("mojang"));
        return runScanningTransformationServices(environment);
    }

    TransformingClassLoader buildTransformingClassLoader(final LaunchPluginHandler pluginHandler, final TransformingClassLoaderBuilder builder, final Environment environment, final ModuleLayerHandler layerHandler) {
        final List<Function<String, Optional<URL>>> classLocatorList = serviceLookup.values().stream().map(TransformationServiceDecorator::getClassLoader).filter(Objects::nonNull).collect(Collectors.toList());
        final var layerInfo = layerHandler.buildLayer(IModuleLayerManager.Layer.GAME, (cf, parents)->new TransformingClassLoader(transformStore, pluginHandler, builder, environment, cf, parents));
        layerHandler.updateLayer(IModuleLayerManager.Layer.PLUGIN, li->li.cl().setFallbackClassLoader(layerInfo.cl()));
        return (TransformingClassLoader) layerInfo.cl();
    }

    private void processArguments(ArgumentHandler argumentHandler, Environment environment) {
        LOGGER.debug(MODLAUNCHER,"Configuring option handling for services");

        argumentHandler.processArguments(environment, this::computeArgumentsForServices, this::offerArgumentResultsToServices);
    }

    private void computeArgumentsForServices(OptionParser parser) {
        serviceLookup.values().stream()
                .map(TransformationServiceDecorator::getService)
                .forEach(service->service.arguments((a, b) -> parser.accepts(service.name() + "." + a, b)));
    }

    private void offerArgumentResultsToServices(OptionSet optionSet, BiFunction<String, OptionSet, ITransformationService.OptionResult> resultHandler) {
        serviceLookup.values().stream()
                .map(TransformationServiceDecorator::getService)
                .forEach(service -> service.argumentValues(resultHandler.apply(service.name(), optionSet)));
    }

    void initialiseServiceTransformers() {
        LOGGER.debug(MODLAUNCHER,"Transformation services loading transformers");

        serviceLookup.values().forEach(s -> s.gatherTransformers(transformStore));
    }

    private void initialiseTransformationServices(Environment environment) {
        LOGGER.debug(MODLAUNCHER,"Transformation services initializing");

        serviceLookup.values().forEach(s -> s.onInitialize(environment));
    }

    private List<ITransformationService.Resource> runScanningTransformationServices(Environment environment) {
        LOGGER.debug(MODLAUNCHER,"Transformation services begin scanning");

        return serviceLookup.values()
                .stream()
                .map(s -> s.runScan(environment))
                .<ITransformationService.Resource>mapMulti(Iterable::forEach)
                .toList();
    }

    private void validateTransformationServices() {
        if (serviceLookup.values().stream().filter(d -> !d.isValid()).count() > 0) {
            final List<ITransformationService> services = serviceLookup.values().stream().filter(d -> !d.isValid()).map(TransformationServiceDecorator::getService).collect(Collectors.toList());
            final String names = services.stream().map(ITransformationService::name).collect(Collectors.joining(","));
            LOGGER.error(MODLAUNCHER,"Found {} services that failed to load : [{}]", services.size(), names);
            throw new InvalidLauncherSetupException("Invalid Services found "+names);
        }
    }

    private void loadTransformationServices(Environment environment) {
        LOGGER.debug(MODLAUNCHER,"Transformation services loading");
        serviceLookup.values().forEach(s -> s.onLoad(environment, serviceLookup.keySet()));
    }

    void discoverServices(final ArgumentHandler.DiscoveryData discoveryData) {
        LOGGER.debug(MODLAUNCHER, "Discovering transformation services");
        var bootLayer = layerHandler.getLayer(IModuleLayerManager.Layer.BOOT).orElseThrow();
        var earlyDiscoveryServices = ServiceLoaderUtils.streamServiceLoader(()->ServiceLoader.load(bootLayer, ITransformerDiscoveryService.class),  sce -> LOGGER.fatal(MODLAUNCHER, "Encountered serious error loading transformation discoverer, expect problems", sce))
                .toList();
        var additionalPaths = earlyDiscoveryServices.stream()
                .map(s->s.candidates(discoveryData.gameDir(), discoveryData.launchTarget()))
                .<SecureJar>mapMulti(Iterable::forEach)
                .toList();
        LOGGER.debug(MODLAUNCHER, "Found additional transformation services from discovery services: {}", ()->additionalPaths.stream().map(SecureJar::name).collect(Collectors.joining()));
        additionalPaths.forEach(np->layerHandler.addToLayer(IModuleLayerManager.Layer.SERVICE, np));
        var serviceLayer = layerHandler.buildLayer(IModuleLayerManager.Layer.SERVICE);
        earlyDiscoveryServices.forEach(s->s.earlyInitialization(discoveryData.launchTarget(), discoveryData.arguments()));
        serviceLookup = ServiceLoaderUtils.streamServiceLoader(()->ServiceLoader.load(serviceLayer.layer(), ITransformationService.class), sce -> LOGGER.fatal(MODLAUNCHER, "Encountered serious error loading transformation service, expect problems", sce))
                .collect(Collectors.toMap(ITransformationService::name, TransformationServiceDecorator::new));
        var modlist = serviceLookup.entrySet().stream().map(e->Map.of(
                "name", e.getKey(),
                "type", "TRANSFORMATIONSERVICE",
                "file", ServiceLoaderUtils.fileNameFor(e.getValue().getClass())
                )).toList();
        Launcher.INSTANCE.environment().getProperty(IEnvironment.Keys.MODLIST.get()).ifPresent(ml->ml.addAll(modlist));
        LOGGER.debug(MODLAUNCHER,"Found transformer services : [{}]", () -> String.join(",",serviceLookup.keySet()));
    }

    public List<ITransformationService.Resource> triggerScanCompletion(IModuleLayerManager moduleLayerManager) {
        return serviceLookup.values().stream()
                .map(tsd->tsd.onCompleteScan(moduleLayerManager))
                .<ITransformationService.Resource>mapMulti(Iterable::forEach)
                .toList();

    }
}
