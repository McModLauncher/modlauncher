package modlauncher;

import cpw.mods.modlauncher.api.LauncherService;
import joptsimple.OptionParser;
import joptsimple.OptionSet;
import joptsimple.OptionSpec;

import java.io.File;
import java.util.List;
import java.util.ServiceLoader;
import java.util.function.Consumer;
import java.util.stream.StreamSupport;

/**
 * Entry point for the ModLauncher.
 *
 */
public class Launcher
{

    private final ServiceLoader<LauncherService> modServices;
    private final String[] args;

    public static void main(String... args)
    {
        new Launcher(args);
    }

    private Launcher(String... args)
    {
        this.args = args;
        modServices = ServiceLoader.load(LauncherService.class);
        run();
    }


    private void run()
    {
        parallelForEach(service -> validate(service.name()));
        final OptionParser parser = new OptionParser();
        parser.allowsUnrecognizedOptions();
        final OptionSpec<String> profileOption = parser.accepts("version", "The version we launched with").withRequiredArg();
        final OptionSpec<File> gameDirOption = parser.accepts("gameDir", "Alternative game directory").withRequiredArg().ofType(File.class);
        final OptionSpec<File> assetsDirOption = parser.accepts("assetsDir", "Assets directory").withRequiredArg().ofType(File.class);

        parallelForEach(service -> service.arguments((a, b) -> parser.accepts(service.name()+":"+a, b)));

        final OptionSpec<String> nonOption = parser.nonOptions();
        final OptionSet options = parser.parse(args);

        parallelForEach(service -> service.argumentValues(optionResults(service.name(), options)));


        ClassLoader l = null;
        parallelForEach(service -> l.addTransformer(service.transformers()));
    }

    private void validate(String name)
    {

    }

    private void parallelForEach(Consumer<LauncherService> modServiceConsumer)
    {
        StreamSupport.stream(modServices.spliterator(), true).forEach(modServiceConsumer);
    }

    private LauncherService.OptionResult optionResults(String serviceName, OptionSet set) {
        return new LauncherService.OptionResult() {
            @Override
            public <V> V value(OptionSpec<V> option) {
                checkOwnership(option);
                return set.valueOf(option);
            }

            @Override
            public <V> List<V> values(OptionSpec<V> option) {
                checkOwnership(option);
                return set.valuesOf(option);
            }

            private <V> void checkOwnership(OptionSpec<V> option) {
                if (! (option.options().stream().allMatch(opt -> opt.startsWith(serviceName + ":") || opt.indexOf(":") == -1)))
                    throw new IllegalArgumentException("Cannot process non-arguments");
            }
        };
    }
}
