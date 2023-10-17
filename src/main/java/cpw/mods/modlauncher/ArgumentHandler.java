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

import cpw.mods.modlauncher.api.*;
import cpw.mods.modlauncher.util.UriConverter;
import joptsimple.*;
import joptsimple.util.*;
import org.jetbrains.annotations.NotNull;

import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.*;
import java.util.*;
import java.util.function.*;

public class ArgumentHandler {
    private String[] args;
    private OptionSet optionSet;
    private OptionSpec<String> profileOption;
    private OptionSpec<Path> gameDirOption;
    private OptionSpec<Path> assetsDirOption;
    private OptionSpec<Path> minecraftJarOption;
    private OptionSpec<String> nonOption;
    private OptionSpec<String> launchTarget;
    private OptionSpec<String> uuidOption;
    private OptionSpec<URI> loggingConfigOption;

    record DiscoveryData(Path gameDir, String launchTarget, List<URI> loggingConfigs, String[] arguments) {}

    DiscoveryData setArgs(String[] args) {
        this.args = args;
        final OptionParser parser = new OptionParser();
        final var gameDir = parser.accepts("gameDir", "Alternative game directory").withRequiredArg().withValuesConvertedBy(new PathConverter(PathProperties.DIRECTORY_EXISTING)).defaultsTo(Path.of("."));
        final var launchTarget = parser.accepts("launchTarget", "LauncherService target to launch").withRequiredArg();
        final var loggingConfig = parser.accepts("loggingConfig", "Log4j configuration files to composite together").withOptionalArg().withValuesConvertedBy(new UriConverter()).withValuesSeparatedBy(',').defaultsTo(getDefaultLoggingConfiguration());
        parser.allowsUnrecognizedOptions();
        final OptionSet optionSet = parser.parse(args);
        return new DiscoveryData(optionSet.valueOf(gameDir), optionSet.valueOf(launchTarget), optionSet.valuesOf(loggingConfig), args);
    }

    void processArguments(Environment env, Consumer<OptionParser> parserConsumer, BiConsumer<OptionSet, BiFunction<String, OptionSet, ITransformationService.OptionResult>> resultConsumer) {
        final OptionParser parser = new OptionParser();
        parser.allowsUnrecognizedOptions();
        profileOption = parser.accepts("version", "The version we launched with").withRequiredArg();
        gameDirOption = parser.accepts("gameDir", "Alternative game directory").withRequiredArg().withValuesConvertedBy(new PathConverter(PathProperties.DIRECTORY_EXISTING)).defaultsTo(Path.of("."));
        assetsDirOption = parser.accepts("assetsDir", "Assets directory").withRequiredArg().withValuesConvertedBy(new PathConverter(PathProperties.DIRECTORY_EXISTING));
        minecraftJarOption = parser.accepts("minecraftJar", "Path to minecraft jar").withRequiredArg().withValuesConvertedBy(new PathConverter(PathProperties.READABLE)).withValuesSeparatedBy(',');
        uuidOption = parser.accepts("uuid", "The UUID of the logging in player").withRequiredArg();
        launchTarget = parser.accepts("launchTarget", "LauncherService target to launch").withRequiredArg();
        loggingConfigOption = parser.accepts("loggingConfig", "Log4j configuration files to composite together").withOptionalArg().withValuesConvertedBy(new UriConverter()).withValuesSeparatedBy(',').defaultsTo(getDefaultLoggingConfiguration());

        parserConsumer.accept(parser);
        nonOption = parser.nonOptions();
        this.optionSet = parser.parse(this.args);
        env.computePropertyIfAbsent(IEnvironment.Keys.VERSION.get(), s -> this.optionSet.valueOf(profileOption));
        env.computePropertyIfAbsent(IEnvironment.Keys.GAMEDIR.get(), f -> this.optionSet.valueOf(gameDirOption));
        env.computePropertyIfAbsent(IEnvironment.Keys.ASSETSDIR.get(), f -> this.optionSet.valueOf(assetsDirOption));
        env.computePropertyIfAbsent(IEnvironment.Keys.LAUNCHTARGET.get(), f -> this.optionSet.valueOf(launchTarget));
        env.computePropertyIfAbsent(IEnvironment.Keys.UUID.get(), f -> this.optionSet.valueOf(uuidOption));
        env.computePropertyIfAbsent(IEnvironment.Keys.LOGGING_CONFIG.get(), f -> this.optionSet.valuesOf(loggingConfigOption));
        resultConsumer.accept(this.optionSet, this::optionResults);
    }

    private static URI getDefaultLoggingConfiguration() {
        try {
            return Objects.requireNonNull(ArgumentHandler.class.getClassLoader().getResource("log4j2.xml")).toURI();
        } catch (URISyntaxException e) {
            throw new RuntimeException(e);
        }
    }

    Path[] getSpecialJars() {
        return this.optionSet.valuesOf(minecraftJarOption).toArray(new Path[0]);
    }

    String getLaunchTarget() {
        return this.optionSet.valueOf(launchTarget);
    }

    private ITransformationService.OptionResult optionResults(String serviceName, OptionSet set) {
        return new ITransformationService.OptionResult() {
            @Override
            public <V> V value(OptionSpec<V> option) {
                checkOwnership(option);
                return set.valueOf(option);
            }

            @Override
            public <V> @NotNull List<V> values(OptionSpec<V> option) {
                checkOwnership(option);
                return set.valuesOf(option);
            }

            private <V> void checkOwnership(OptionSpec<V> option) {
                if (!(option.options().stream().allMatch(opt -> opt.startsWith(serviceName + ".") || !opt.contains(".")))) {
                    throw new IllegalArgumentException("Cannot process non-arguments");
                }
            }
        };
    }

    public String[] buildArgumentList() {
        ArrayList<String> args = new ArrayList<>();
        addOptionToString(profileOption, optionSet, args);
        addOptionToString(gameDirOption, optionSet, args);
        addOptionToString(assetsDirOption, optionSet, args);
        addOptionToString(uuidOption, optionSet, args);
        List<?> nonOptionList = this.optionSet.nonOptionArguments();
        nonOptionList.stream().map(Object::toString).forEach(args::add);
        return args.toArray(new String[0]);
    }

    private void addOptionToString(OptionSpec<?> option, OptionSet optionSet, List<String> appendTo) {
        if (optionSet.has(option)) {
            appendTo.add("--"+option.options().get(0));
            appendTo.add(option.value(optionSet).toString());
        }
    }
}
