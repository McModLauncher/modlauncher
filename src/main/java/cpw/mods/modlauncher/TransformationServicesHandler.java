/*
 * Modlauncher - utility to launch Minecraft-like game environments with runtime transformation
 * Copyright ©2016-2017 cpw and others
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301  USA
 */

package cpw.mods.modlauncher;

import cpw.mods.modlauncher.api.*;
import joptsimple.*;

import java.io.*;
import java.util.*;
import java.util.function.*;
import java.util.stream.*;

import static cpw.mods.modlauncher.Logging.*;
import static cpw.mods.modlauncher.ServiceLoaderStreamUtils.*;

public class TransformationServicesHandler {
    private final ServiceLoader<ITransformationService> transformationServices;
    final Map<String, TransformationServiceDecorator> serviceLookup;
    private final TransformStore transformStore;

    TransformationServicesHandler(TransformStore transformStore) {
        transformationServices = ServiceLoader.load(ITransformationService.class);
        launcherLog.info("Found services : {}", () -> ServiceLoaderStreamUtils.toList(transformationServices));

        serviceLookup = StreamSupport.stream(transformationServices.spliterator(), false)
                .collect(Collectors.toMap(ITransformationService::name, TransformationServiceDecorator::new));

        this.transformStore = transformStore;
    }

    void loadTransformationServices(ArgumentHandler argumentHandler, Environment environment)
    {
        loadTransformationServices(environment);
        validateTransformationServices();

        processArguments(argumentHandler, environment);
    }

    void initializeTransformationServices(Environment environment)
    {
        initialiseTransformationServices(environment);
        initialiseServiceTransformers();
    }

    TransformingClassLoader buildTransformingClassLoader(ClassCache classCache, File... specialJars) {
        return new TransformingClassLoader(transformStore, classCache, specialJars);
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
