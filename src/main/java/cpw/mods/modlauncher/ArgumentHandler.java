package cpw.mods.modlauncher;

import cpw.mods.modlauncher.api.Environment;
import cpw.mods.modlauncher.api.LauncherService;
import joptsimple.OptionParser;
import joptsimple.OptionSet;
import joptsimple.OptionSpec;

import javax.annotation.Nonnull;
import java.io.File;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Consumer;

/**
 * Handle command line arguments
 */
public class ArgumentHandler
{
    private String[] args;
    private OptionSet optionSet;
    private OptionSpec<String> profileOption;
    private OptionSpec<File> gameDirOption;
    private OptionSpec<File> assetsDirOption;
    private OptionSpec<File> minecraftJarOption;
    private OptionSpec<String> nonOption;

    void setArgs(String[] args)
    {
        this.args = args;
    }

    File handleArguments(EnvironmentImpl env, Consumer<OptionParser> parserConsumer, BiConsumer<OptionSet, BiFunction<String, OptionSet, LauncherService.OptionResult>> resultConsumer)
    {
        final OptionParser parser = new OptionParser();
        parser.allowsUnrecognizedOptions();
        profileOption = parser.accepts("version", "The version we launched with").withRequiredArg();
        gameDirOption = parser.accepts("gameDir", "Alternative game directory").withRequiredArg().ofType(File.class);
        assetsDirOption = parser.accepts("assetsDir", "Assets directory").withRequiredArg().ofType(File.class);
        minecraftJarOption = parser.accepts("minecraftJar", "Path to minecraft jar").withRequiredArg().ofType(File.class);

        parserConsumer.accept(parser);
        nonOption = parser.nonOptions();
        this.optionSet = parser.parse(this.args);
        env.getMap().computeIfAbsent(Environment.Keys.VERSION.get(), s -> this.optionSet.valueOf(profileOption));
        env.getMap().computeIfAbsent(Environment.Keys.GAMEDIR.get(), f -> this.optionSet.valueOf(gameDirOption));
        env.getMap().computeIfAbsent(Environment.Keys.ASSETSDIR.get(), f -> this.optionSet.valueOf(assetsDirOption));
        resultConsumer.accept(this.optionSet, this::optionResults);
        return this.optionSet.valueOf(minecraftJarOption);
    }

    private LauncherService.OptionResult optionResults(String serviceName, OptionSet set)
    {
        return new LauncherService.OptionResult()
        {
            @Nonnull
            @Override
            public <V> V value(OptionSpec<V> option)
            {
                checkOwnership(option);
                return set.valueOf(option);
            }

            @Nonnull
            @Override
            public <V> List<V> values(OptionSpec<V> option)
            {
                checkOwnership(option);
                return set.valuesOf(option);
            }

            private <V> void checkOwnership(OptionSpec<V> option)
            {
                if (!(option.options().stream().allMatch(opt -> opt.startsWith(serviceName + ".") || !opt.contains("."))))
                {
                    throw new IllegalArgumentException("Cannot process non-arguments");
                }
            }
        };
    }
}
