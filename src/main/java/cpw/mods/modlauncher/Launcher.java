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
import org.slf4j.LoggerFactory;
import cpw.mods.modlauncher.serviceapi.ILaunchPluginService;

import java.nio.file.Path;
import java.util.*;
import java.util.function.BiFunction;
import java.util.stream.Collectors;

import static cpw.mods.modlauncher.LogHelper.*;

/**
 * Entry point for the ModLauncher.
 */
public class Launcher {
    public static Launcher INSTANCE;
    private final TypesafeMap blackboard;
    private final TransformationServicesHandler transformationServicesHandler;
    private final Environment environment;
    private final TransformStore transformStore;
    private final NameMappingServiceHandler nameMappingServiceHandler;
    private final ArgumentHandler argumentHandler;
    private final LaunchServiceHandler launchService;
    private final LaunchPluginHandler launchPlugins;
    private final ModuleLayerHandler moduleLayerHandler;
    private TransformingClassLoader classLoader;

    private Launcher() {
        INSTANCE = this;
        LogHelper.info(MODLAUNCHER, "ModLauncher {} starting: java version {} by {}",
                        ()->IEnvironment.class.getPackage().getImplementationVersion(),
                        () -> System.getProperty("java.version"),
                        ()->System.getProperty("java.vendor"));
        this.moduleLayerHandler = new ModuleLayerHandler();
        this.launchService = new LaunchServiceHandler(this.moduleLayerHandler);
        this.blackboard = new TypesafeMap();
        this.environment = new Environment(this);
        environment.computePropertyIfAbsent(IEnvironment.Keys.MLSPEC_VERSION.get(), s->IEnvironment.class.getPackage().getSpecificationVersion());
        environment.computePropertyIfAbsent(IEnvironment.Keys.MLIMPL_VERSION.get(), s->IEnvironment.class.getPackage().getImplementationVersion());
        environment.computePropertyIfAbsent(IEnvironment.Keys.MODLIST.get(), s->new ArrayList<>());
        environment.computePropertyIfAbsent(IEnvironment.Keys.SECURED_JARS_ENABLED.get(), k-> ProtectionDomainHelper.canHandleSecuredJars());
        this.transformStore = new TransformStore();
        this.transformationServicesHandler = new TransformationServicesHandler(this.transformStore, this.moduleLayerHandler);
        this.argumentHandler = new ArgumentHandler();
        this.nameMappingServiceHandler = new NameMappingServiceHandler(this.moduleLayerHandler);
        this.launchPlugins = new LaunchPluginHandler(this.moduleLayerHandler);
    }

    public static void main(String... args) {
        if (System.getProperty("java.vendor").contains("OpenJ9")) {
            System.err.printf("""
            You are attempting to run with an unsupported Java Virtual Machine : %s
            Please visit https://adoptopenjdk.net and install the HotSpot variant.
            OpenJ9 is incompatible with several of the transformation behaviours that we rely on to work.
            """, System.getProperty("java.vendor"));
            throw new IllegalStateException("Open J9 is not supported");
        }
        LogHelper.info(MODLAUNCHER,"ModLauncher running: args {}", () -> LaunchServiceHandler.hideAccessToken(args));
        new Launcher().run(args);
    }

    public final TypesafeMap blackboard() {
        return blackboard;
    }

    private void run(String... args) {
        final Path gameDir = this.argumentHandler.setArgs(args);
        this.transformationServicesHandler.discoverServices(gameDir);
        final var scanResults = this.transformationServicesHandler.initializeTransformationServices(this.argumentHandler, this.environment, this.nameMappingServiceHandler);
        var bylayer = scanResults.stream().collect(Collectors.groupingBy(ITransformationService.Resource::target));
        bylayer.getOrDefault(IModuleLayerManager.Layer.PLUGIN, List.of())
                .stream()
                .<SecureJar>mapMulti((resource, action) -> resource.resources().forEach(action))
                .forEach(np->this.moduleLayerHandler.addToLayer(IModuleLayerManager.Layer.PLUGIN, np));
        this.moduleLayerHandler.buildLayer(IModuleLayerManager.Layer.PLUGIN);
        final var gameResults = this.transformationServicesHandler.triggerScanCompletion(this.moduleLayerHandler);
        bylayer = gameResults.stream().collect(Collectors.groupingBy(ITransformationService.Resource::target));
        var gamecontents = bylayer.getOrDefault(IModuleLayerManager.Layer.GAME, List.of()).stream()
                .<SecureJar>mapMulti((resource, action) -> resource.resources().forEach(action))
                .toList();
        gamecontents.forEach(j->this.moduleLayerHandler.addToLayer(IModuleLayerManager.Layer.GAME, j));
        this.transformationServicesHandler.initialiseServiceTransformers();
        this.launchPlugins.offerScanResultsToPlugins(gamecontents);
        this.launchService.validateLaunchTarget(this.argumentHandler);
        final TransformingClassLoaderBuilder classLoaderBuilder = this.launchService.identifyTransformationTargets(this.argumentHandler);
        this.classLoader = this.transformationServicesHandler.buildTransformingClassLoader(this.launchPlugins, classLoaderBuilder, this.environment, this.moduleLayerHandler);
        Thread.currentThread().setContextClassLoader(this.classLoader);
        this.launchService.launch(this.argumentHandler, this.moduleLayerHandler.getLayer(IModuleLayerManager.Layer.GAME).orElseThrow(), this.classLoader, this.launchPlugins);
    }

    public Environment environment() {
        return this.environment;
    }

    Optional<ILaunchPluginService> findLaunchPlugin(final String name) {
        return launchPlugins.get(name);
    }

    Optional<ILaunchHandlerService> findLaunchHandler(final String name) {
        return launchService.findLaunchHandler(name);
    }

    Optional<BiFunction<INameMappingService.Domain, String, String>> findNameMapping(final String targetMapping) {
        return nameMappingServiceHandler.findNameTranslator(targetMapping);
    }

    public Optional<IModuleLayerManager> findLayerManager() {
        return Optional.ofNullable(this.moduleLayerHandler);
    }
}
