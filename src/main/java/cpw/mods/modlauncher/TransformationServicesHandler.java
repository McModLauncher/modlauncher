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

import cpw.mods.gross.Java9ClassLoaderUtil;
import cpw.mods.modlauncher.api.*;
import cpw.mods.modlauncher.serviceapi.ITransformerDiscoveryService;
import joptsimple.*;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.annotation.Nullable;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.*;
import java.util.*;
import java.util.function.*;
import java.util.stream.*;

import static cpw.mods.modlauncher.LogMarkers.*;
import static cpw.mods.modlauncher.ServiceLoaderStreamUtils.*;

class TransformationServicesHandler {
    private static final Logger LOGGER = LogManager.getLogger();
    private ServiceLoader<ITransformationService> transformationServices;
    private Map<String, TransformationServiceDecorator> serviceLookup;
    private final TransformStore transformStore;

    TransformationServicesHandler(TransformStore transformStore) {
        this.transformStore = transformStore;
    }

    private static <I, R> Function<I,Optional<R>> alternate(@Nullable Function<I, Optional<R>> first, @Nullable Function<I, Optional<R>> second) {
        if (second == null) return first;
        if (first == null) return second;
        return input -> Optional.ofNullable(first.apply(input).orElseGet(() -> second.apply(input).orElse(null)));
    }

    List<Map.Entry<String, Path>> initializeTransformationServices(ArgumentHandler argumentHandler, Environment environment, final NameMappingServiceHandler nameMappingServiceHandler) {
        loadTransformationServices(environment);
        validateTransformationServices();
        processArguments(argumentHandler, environment);
        initialiseTransformationServices(environment);
        // force the naming to "mojang" if nothing has been populated during transformer setup
        environment.computePropertyIfAbsent(IEnvironment.Keys.NAMING.get(), a-> "mojang");
        nameMappingServiceHandler.bindNamingServices(environment.getProperty(Environment.Keys.NAMING.get()).orElse("mojang"));
        final List<Map.Entry<String, Path>> scanResults = runScanningTransformationServices(environment);
        initialiseServiceTransformers();
        return scanResults;
    }

    TransformingClassLoader buildTransformingClassLoader(final LaunchPluginHandler pluginHandler, final TransformingClassLoaderBuilder builder, final Environment environment) {
        final List<Function<String, Optional<URL>>> classLocatorList = serviceLookup.values().stream().map(TransformationServiceDecorator::getClassLoader).filter(Objects::nonNull).collect(Collectors.toList());
        Function<String, Optional<URL>> classBytesLocator = builder.getClassBytesLocator();

        for (Function<String, Optional<URL>> transformerClassLocator : classLocatorList) {
            classBytesLocator = alternate(classBytesLocator, transformerClassLocator);
        }

        builder.setClassBytesLocator(classBytesLocator);
        return new TransformingClassLoader(transformStore, pluginHandler, builder, environment);
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

    private List<Map.Entry<String, Path>> runScanningTransformationServices(Environment environment) {
        LOGGER.debug(MODLAUNCHER,"Transformation services begin scanning");

        return serviceLookup.values()
                .stream()
                .flatMap(s -> s.runScan(environment).stream())
                .collect(Collectors.toList());
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

    void discoverServices(final Path gameDir) {
        LOGGER.debug(MODLAUNCHER, "Discovering transformation services");
        final ServiceLoader<ITransformerDiscoveryService> discoveryServices = errorHandlingServiceLoader(ITransformerDiscoveryService.class, serviceConfigurationError -> LOGGER.fatal(MODLAUNCHER, "Encountered serious error loading transformation discoverer, expect problems", serviceConfigurationError));
        final List<Path> additionalPaths = map(discoveryServices, s -> s.candidates(gameDir)).flatMap(Collection::stream).collect(Collectors.toList());
        LOGGER.debug(MODLAUNCHER, "Found additional transformation services from discovery services: {}", additionalPaths);
        TransformerClassLoader cl = new TransformerClassLoader(Java9ClassLoaderUtil.getSystemClassPathURLs());
        additionalPaths.stream().map(LamdbaExceptionUtils.rethrowFunction(p->p.toUri().toURL())).forEach(cl::addURL);
        transformationServices = ServiceLoaderStreamUtils.errorHandlingServiceLoader(ITransformationService.class, cl, serviceConfigurationError -> LOGGER.fatal(MODLAUNCHER, "Encountered serious error loading transformation service, expect problems", serviceConfigurationError));
        serviceLookup = ServiceLoaderStreamUtils.toMap(transformationServices, ITransformationService::name, TransformationServiceDecorator::new);
        final List<Map<String, String>> modlist = Launcher.INSTANCE.environment().getProperty(IEnvironment.Keys.MODLIST.get()).orElseThrow(()->new RuntimeException("The MODLIST isn't set, huh?"));
        serviceLookup.forEach((name, deco)->{
            HashMap<String,String> mod = new HashMap<>();
            mod.put("name", name);
            mod.put("type", "TRANSFORMATIONSERVICE");
            String fName = deco.getService().getClass().getProtectionDomain().getCodeSource().getLocation().getFile();
            mod.put("file", fName.substring(fName.lastIndexOf("/")));
            modlist.add(mod);
        });
        LOGGER.debug(MODLAUNCHER,"Found transformer services : [{}]", () -> String.join(",",serviceLookup.keySet()));

    }


    private static class TransformerClassLoader extends URLClassLoader {
        TransformerClassLoader(final URL[] urls) {
            super(urls);
        }

        @Override
        protected void addURL(final URL url) {
            super.addURL(url);
        }
    }
}
