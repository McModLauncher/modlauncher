package cpw.mods.modlauncher;

import cpw.mods.modlauncher.api.IEnvironment;
import cpw.mods.modlauncher.api.ITransformationService;
import joptsimple.OptionParser;
import joptsimple.OptionSet;
import joptsimple.OptionSpec;

import javax.annotation.Nonnull;
import java.io.File;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Consumer;

public class ArgumentHandler
{
    private String[] args;
    private OptionSet optionSet;
    private OptionSpec<String> profileOption;
    private OptionSpec<File> gameDirOption;
    private OptionSpec<File> assetsDirOption;
    private OptionSpec<File> minecraftJarOption;
    private OptionSpec<String> nonOption;
    private OptionSpec<String> launchTarget;

    void setArgs(String[] args)
    {
        this.args = args;
    }

    void processArguments(Environment env, Consumer<OptionParser> parserConsumer, BiConsumer<OptionSet, BiFunction<String, OptionSet, ITransformationService.OptionResult>> resultConsumer)
    {
        final OptionParser parser = new OptionParser();
        parser.allowsUnrecognizedOptions();
        profileOption = parser.accepts("version", "The version we launched with").withRequiredArg();
        gameDirOption = parser.accepts("gameDir", "Alternative game directory").withRequiredArg().ofType(File.class);
        assetsDirOption = parser.accepts("assetsDir", "Assets directory").withRequiredArg().ofType(File.class);
        minecraftJarOption = parser.accepts("minecraftJar", "Path to minecraft jar").withRequiredArg().ofType(File.class);
        launchTarget = parser.accepts("launchTarget", "LauncherService target to launch").withRequiredArg();

        parserConsumer.accept(parser);
        nonOption = parser.nonOptions();
        this.optionSet = parser.parse(this.args);
        env.getAll().computeIfAbsent(IEnvironment.Keys.VERSION.get(), s -> this.optionSet.valueOf(profileOption));
        env.getAll().computeIfAbsent(IEnvironment.Keys.GAMEDIR.get(), f -> this.optionSet.valueOf(gameDirOption));
        env.getAll().computeIfAbsent(IEnvironment.Keys.ASSETSDIR.get(), f -> this.optionSet.valueOf(assetsDirOption));
        resultConsumer.accept(this.optionSet, this::optionResults);
    }

    File getSpecialJars()
    {
        return this.optionSet.valueOf(minecraftJarOption);
    }

    String getLaunchTarget()
    {
        return this.optionSet.valueOf(launchTarget);
    }

    private ITransformationService.OptionResult optionResults(String serviceName, OptionSet set)
    {
        return new ITransformationService.OptionResult()
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

    public String[] buildArgumentList()
    {
        String[] ret = new String[this.args.length];
        System.arraycopy(this.args, 0, ret, 0, this.args.length);
        return ret;
    }
}
