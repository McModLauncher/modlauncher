package cpw.mods.modlauncher;

import cpw.mods.modlauncher.api.*;
import joptsimple.*;
import joptsimple.util.*;

import javax.annotation.*;
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

    void setArgs(String[] args) {
        this.args = args;
    }

    void processArguments(Environment env, Consumer<OptionParser> parserConsumer, BiConsumer<OptionSet, BiFunction<String, OptionSet, ITransformationService.OptionResult>> resultConsumer) {
        final OptionParser parser = new OptionParser();
        parser.allowsUnrecognizedOptions();
        profileOption = parser.accepts("version", "The version we launched with").withRequiredArg();
        gameDirOption = parser.accepts("gameDir", "Alternative game directory").withRequiredArg().withValuesConvertedBy(new PathConverter(PathProperties.DIRECTORY_EXISTING));
        assetsDirOption = parser.accepts("assetsDir", "Assets directory").withRequiredArg().withValuesConvertedBy(new PathConverter(PathProperties.DIRECTORY_EXISTING));
        minecraftJarOption = parser.accepts("minecraftJar", "Path to minecraft jar").withRequiredArg().withValuesConvertedBy(new PathConverter(PathProperties.READABLE)).withValuesSeparatedBy(',');
        launchTarget = parser.accepts("launchTarget", "LauncherService target to launch").withRequiredArg();

        parserConsumer.accept(parser);
        nonOption = parser.nonOptions();
        this.optionSet = parser.parse(this.args);
        env.computePropertyIfAbsent(IEnvironment.Keys.VERSION.get(), s -> this.optionSet.valueOf(profileOption));
        env.computePropertyIfAbsent(IEnvironment.Keys.GAMEDIR.get(), f -> this.optionSet.valueOf(gameDirOption));
        env.computePropertyIfAbsent(IEnvironment.Keys.ASSETSDIR.get(), f -> this.optionSet.valueOf(assetsDirOption));
        env.computePropertyIfAbsent(IEnvironment.Keys.LAUNCHTARGET.get(), f -> this.optionSet.valueOf(launchTarget));
        resultConsumer.accept(this.optionSet, this::optionResults);
    }

    Path[] getSpecialJars() {
        return this.optionSet.valuesOf(minecraftJarOption).toArray(new Path[0]);
    }

    String getLaunchTarget() {
        return this.optionSet.valueOf(launchTarget);
    }

    private ITransformationService.OptionResult optionResults(String serviceName, OptionSet set) {
        return new ITransformationService.OptionResult() {
            @Nonnull
            @Override
            public <V> V value(OptionSpec<V> option) {
                checkOwnership(option);
                return set.valueOf(option);
            }

            @Nonnull
            @Override
            public <V> List<V> values(OptionSpec<V> option) {
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
