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
import cpw.mods.modlauncher.log.MarkerLogLevelFilter;
import cpw.mods.modlauncher.util.LoggingUtils;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import cpw.mods.modlauncher.serviceapi.ILaunchPluginService;
import org.apache.logging.log4j.core.Filter;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.config.*;
import org.apache.logging.log4j.core.config.builder.api.ConfigurationBuilderFactory;
import org.apache.logging.log4j.core.config.composite.CompositeConfiguration;
import org.apache.logging.log4j.core.filter.MarkerFilter;

import java.net.URI;
import java.nio.file.Path;
import java.util.*;
import java.util.function.BiFunction;
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
    private final NameMappingServiceHandler nameMappingServiceHandler;
    private final ArgumentHandler argumentHandler;
    private final LaunchServiceHandler launchService;
    private final LaunchPluginHandler launchPlugins;
    private final ModuleLayerHandler moduleLayerHandler;
    private TransformingClassLoader classLoader;

    private Launcher() {
        INSTANCE = this;
        LogManager.getLogger().info(MODLAUNCHER,"ModLauncher {} starting: java version {} by {}; OS {} arch {} version {}", ()->IEnvironment.class.getPackage().getImplementationVersion(),  () -> System.getProperty("java.version"), ()->System.getProperty("java.vendor"), ()->System.getProperty("os.name"), ()->System.getProperty("os.arch"), ()->System.getProperty("os.version"));
        this.moduleLayerHandler = new ModuleLayerHandler();
        this.launchService = new LaunchServiceHandler(this.moduleLayerHandler);
        this.blackboard = new TypesafeMap();
        this.environment = new Environment(this);
        environment.computePropertyIfAbsent(IEnvironment.Keys.MLSPEC_VERSION.get(), s->IEnvironment.class.getPackage().getSpecificationVersion());
        environment.computePropertyIfAbsent(IEnvironment.Keys.MLIMPL_VERSION.get(), s->IEnvironment.class.getPackage().getImplementationVersion());
        environment.computePropertyIfAbsent(IEnvironment.Keys.MODLIST.get(), s->new ArrayList<>());
        environment.computePropertyIfAbsent(IEnvironment.Keys.SECURED_JARS_ENABLED.get(), k-> ProtectionDomainHelper.canHandleSecuredJars());
        environment.computePropertyIfAbsent(IEnvironment.Keys.LOGGING_CONFIG.get(), k -> new ArrayList<>(LoggingUtils.getConfigurationSources(this.moduleLayerHandler.getLayer(IModuleLayerManager.Layer.BOOT).orElseThrow())));
        this.transformStore = new TransformStore();
        this.transformationServicesHandler = new TransformationServicesHandler(this.transformStore, this.moduleLayerHandler);
        this.argumentHandler = new ArgumentHandler();
        this.nameMappingServiceHandler = new NameMappingServiceHandler(this.moduleLayerHandler);
        this.launchPlugins = new LaunchPluginHandler(this.moduleLayerHandler);
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
        new Launcher().run(args);
    }

    public final TypesafeMap blackboard() {
        return blackboard;
    }

    private void run(String... args) {
        final ArgumentHandler.DiscoveryData discoveryData = this.argumentHandler.setArgs(args);
        reconfigureLogger(discoveryData.loggingConfigs());
        this.transformationServicesHandler.discoverServices(discoveryData);
        reconfigureLogger(discoveryData.loggingConfigs());
        final var scanResults = this.transformationServicesHandler.initializeTransformationServices(this.argumentHandler, this.environment, this.nameMappingServiceHandler)
                .stream().collect(Collectors.groupingBy(ITransformationService.Resource::target));
        scanResults.getOrDefault(IModuleLayerManager.Layer.PLUGIN, List.of())
                .stream()
                .<SecureJar>mapMulti((resource, action) -> resource.resources().forEach(action))
                .forEach(np->this.moduleLayerHandler.addToLayer(IModuleLayerManager.Layer.PLUGIN, np));
        final var pluginLayer = this.moduleLayerHandler.buildLayer(IModuleLayerManager.Layer.PLUGIN);
        var sources = LoggingUtils.getConfigurationSources(pluginLayer.layer());
        this.environment.getProperty(IEnvironment.Keys.LOGGING_CONFIG.get()).ifPresent(lc -> lc.addAll(sources));
        reconfigureLogger(discoveryData.loggingConfigs());
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
        final TransformingClassLoaderBuilder classLoaderBuilder = this.launchService.identifyTransformationTargets(this.argumentHandler);
        this.classLoader = this.transformationServicesHandler.buildTransformingClassLoader(this.launchPlugins, classLoaderBuilder, this.environment, this.moduleLayerHandler);
        reconfigureLogger(discoveryData.loggingConfigs());
        Thread.currentThread().setContextClassLoader(this.classLoader);
        this.launchService.launch(this.argumentHandler, this.moduleLayerHandler.getLayer(IModuleLayerManager.Layer.GAME).orElseThrow(), this.classLoader, this.launchPlugins);
    }

    private void reconfigureLogger(List<URI> additionalConfigurationFiles) {
        final var configurations = this.environment.getProperty(IEnvironment.Keys.LOGGING_CONFIG.get()).orElseThrow().stream()
            .map(ConfigurationSource::fromUri)
            .map(source -> ConfigurationFactory.getInstance().getConfiguration(LoggerContext.getContext(), source))
            .map(AbstractConfiguration.class::cast)
            .collect(Collectors.toList());

        additionalConfigurationFiles.stream()
            .map(ConfigurationSource::fromUri)
            .map(source -> ConfigurationFactory.getInstance().getConfiguration(LoggerContext.getContext(), source))
            .map(AbstractConfiguration.class::cast)
            .forEach(configurations::add);

        final var levelConfigBuilder = ConfigurationBuilderFactory.newConfigurationBuilder()
            .setConfigurationName("MODLAUNCHER-LOGLEVELS");
        System.getProperties().entrySet().stream()
            .map(entry -> (Map.Entry<String, String>)(Map.Entry<?,?>)entry)
            .filter(entry -> entry.getKey().startsWith("logging.loglevel."))
            .forEach(entry -> {
                final var loggerName = entry.getKey().substring("logging.loglevel.".length());
                final var level = Level.getLevel(entry.getValue());
                if (loggerName.equals("default")) {
                    levelConfigBuilder.add(levelConfigBuilder.newRootLogger(level));
                } else {
                    levelConfigBuilder.add(levelConfigBuilder.newLogger(loggerName, level));
                }
            });

        final var markerConfigBuilder = ConfigurationBuilderFactory.newConfigurationBuilder()
            .setConfigurationName("MODLAUNCHER-MARKERS");
        System.getProperties().entrySet().stream()
            .map(entry -> (Map.Entry<String, String>)(Map.Entry<?, ?>)entry)
            .filter(entry -> entry.getKey().startsWith("logging.marker."))
            .forEach(entry -> {
                final var markerName = entry.getKey().substring("logging.marker.".length());
                final var minimumLevel = Level.getLevel(entry.getValue());
                markerConfigBuilder.add(markerConfigBuilder.newFilter("MarkerLogLevelFilter", Filter.Result.ACCEPT, Filter.Result.DENY)
                    .addAttribute(MarkerLogLevelFilter.ATTR_MARKER, markerName)
                    .addAttribute(MarkerLogLevelFilter.ATTR_MINIMUM_LEVEL, minimumLevel));
            });

        // These are the default markers; they have to be specified at the very end so that they have the lowest priority.
        markerConfigBuilder.add(markerConfigBuilder.newFilter("MarkerFilter", Filter.Result.DENY, Filter.Result.NEUTRAL)
                .addAttribute(MarkerFilter.ATTR_MARKER, "NETWORK_PACKETS"))
            .add(markerConfigBuilder.newFilter("MarkerFilter", Filter.Result.DENY, Filter.Result.NEUTRAL)
                .addAttribute(MarkerFilter.ATTR_MARKER, "CLASSLOADING"))
            .add(markerConfigBuilder.newFilter("MarkerFilter", Filter.Result.DENY, Filter.Result.NEUTRAL)
                .addAttribute(MarkerFilter.ATTR_MARKER, "LAUNCHPLUGIN"))
            .add(markerConfigBuilder.newFilter("MarkerFilter", Filter.Result.DENY, Filter.Result.NEUTRAL)
                .addAttribute(MarkerFilter.ATTR_MARKER, "CLASSDUMP"));

        configurations.add(levelConfigBuilder.build());
        configurations.add(markerConfigBuilder.build());
        Configurator.reconfigure(new CompositeConfiguration(configurations));
        LogManager.getLogger().trace(MODLAUNCHER, "Logger reconfigured from {} sources", configurations.size());
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
