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
import org.apache.logging.log4j.LogManager;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static cpw.mods.modlauncher.LogMarkers.*;

/**
 * Entry point for the ModLauncher.
 */
public class Launcher {
    public static Launcher INSTANCE;
    private final TypesafeMap blackboard;
    private final TransformationServicesHandler transformationServicesHandler;
    private final Environment environment;
    private final TransformStore transformStore;
    private final LaunchServiceHandler launchService;
    private final LaunchPluginHandler launchPlugins;
    private final ModuleLayerHandler moduleLayerHandler;
    private TransformingClassLoader classLoader;
    private ArgumentHandler argumentHandler;

    public Launcher() {
        LogManager.getLogger().info(MODLAUNCHER,"ModLauncher {} starting: java version {} by {}; OS {} arch {} version {}", ()->IEnvironment.class.getPackage().getImplementationVersion(),  () -> System.getProperty("java.version"), ()->System.getProperty("java.vendor"), ()->System.getProperty("os.name"), ()->System.getProperty("os.arch"), ()->System.getProperty("os.version"));
        this.moduleLayerHandler = new ModuleLayerHandler();
        this.launchService = new LaunchServiceHandler(this.moduleLayerHandler);
        this.blackboard = new TypesafeMap();
        this.transformStore = new TransformStore();
        this.transformationServicesHandler = new TransformationServicesHandler(this.transformStore, this.moduleLayerHandler);
        this.launchPlugins = new LaunchPluginHandler(this.moduleLayerHandler);
        this.environment = new Environment(
                launchPlugins::get,
                launchService::findLaunchHandler,
                moduleLayerHandler
        );
        environment.computePropertyIfAbsent(IEnvironment.Keys.MLSPEC_VERSION.get(), s->IEnvironment.class.getPackage().getSpecificationVersion());
        environment.computePropertyIfAbsent(IEnvironment.Keys.MLIMPL_VERSION.get(), s->IEnvironment.class.getPackage().getImplementationVersion());
        environment.computePropertyIfAbsent(IEnvironment.Keys.MODLIST.get(), s->new ArrayList<>());
    }

    public Launcher(TransformationServicesHandler transformationServicesHandler,
                    Environment environment,
                    TransformStore transformStore,
                    LaunchServiceHandler launchService,
                    LaunchPluginHandler launchPlugins,
                    ModuleLayerHandler moduleLayerHandler) {
        this.blackboard = new TypesafeMap();
        this.transformationServicesHandler = transformationServicesHandler;
        this.environment = environment;
        this.transformStore = transformStore;
        this.launchService = launchService;
        this.launchPlugins = launchPlugins;
        this.moduleLayerHandler = moduleLayerHandler;
    }

    public static void main(String... args) {
        var props = System.getProperties();
        if (props.getProperty("java.vm.name").contains("OpenJ9")) {
            System.err.printf("""
            WARNING: OpenJ9 is detected. This is definitely unsupported and you may encounter issues and significantly worse performance.
            For support and performance reasons, we recommend installing a temurin JVM from https://adoptium.net/
            JVM information: %s %s %s
            """, props.getProperty("java.vm.vendor"), props.getProperty("java.vm.name"), props.getProperty("java.vm.version"));
        }
        LogManager.getLogger().info(MODLAUNCHER,"ModLauncher running: args {}", () -> LaunchServiceHandler.hideAccessToken(args));
        LogManager.getLogger().info(MODLAUNCHER, "JVM identified as {} {} {}", props.getProperty("java.vm.vendor"), props.getProperty("java.vm.name"), props.getProperty("java.vm.version"));
        var launcher = new Launcher();
        INSTANCE = launcher;
        launcher.run(args);
    }

    public final TypesafeMap blackboard() {
        return blackboard;
    }

    private void run(String... args) {
        this.argumentHandler = new ArgumentHandler(args);
        this.transformationServicesHandler.discoverServices(DiscoveryData.create(args));
        final var scanResults = this.transformationServicesHandler.initializeTransformationServices(this.argumentHandler, this.environment)
                .stream().collect(Collectors.groupingBy(ITransformationService.Resource::target));
        scanResults.getOrDefault(IModuleLayerManager.Layer.PLUGIN, List.of())
                .stream()
                .<SecureJar>mapMulti((resource, action) -> resource.resources().forEach(action))
                .forEach(np->this.moduleLayerHandler.addToLayer(IModuleLayerManager.Layer.PLUGIN, np));
        this.moduleLayerHandler.buildLayer(IModuleLayerManager.Layer.PLUGIN);
        final var gameResults = this.transformationServicesHandler.triggerScanCompletion(this.moduleLayerHandler)
                .stream().collect(Collectors.groupingBy(ITransformationService.Resource::target));
        final var gameContents = Stream.of(scanResults, gameResults)
                .flatMap(m -> m.getOrDefault(IModuleLayerManager.Layer.GAME, List.of()).stream())
                .<SecureJar>mapMulti((resource, action) -> resource.resources().forEach(action))
                .toList();
        gameContents.forEach(j->this.moduleLayerHandler.addToLayer(IModuleLayerManager.Layer.GAME, j));
        this.transformationServicesHandler.initialiseServiceTransformers();
        this.launchPlugins.offerScanResultsToPlugins(gameContents);
        this.launchService.validateLaunchTarget(this.argumentHandler);
        this.classLoader = this.transformationServicesHandler.buildTransformingClassLoader(this.launchPlugins, this.environment, this.moduleLayerHandler);
        Thread.currentThread().setContextClassLoader(this.classLoader);
        this.launchService.launch(this.argumentHandler, this.moduleLayerHandler.getLayer(IModuleLayerManager.Layer.GAME).orElseThrow(), this.classLoader, this.launchPlugins);
    }

    public Environment environment() {
        return this.environment;
    }

    public Optional<IModuleLayerManager> findLayerManager() {
        return Optional.ofNullable(this.moduleLayerHandler);
    }
}
