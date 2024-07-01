package cpw.mods.modlauncher;

import joptsimple.OptionParser;
import joptsimple.OptionSet;
import joptsimple.util.PathConverter;
import joptsimple.util.PathProperties;

import java.nio.file.Path;

public record DiscoveryData(Path gameDir, String launchTarget, String[] arguments) {
    public static DiscoveryData create(String[] programArgs) {
        final OptionParser parser = new OptionParser();
        final var gameDir = parser.accepts("gameDir", "Alternative game directory").withRequiredArg().withValuesConvertedBy(new PathConverter(PathProperties.DIRECTORY_EXISTING)).defaultsTo(Path.of("."));
        final var launchTarget = parser.accepts("launchTarget", "LauncherService target to launch").withRequiredArg();
        parser.allowsUnrecognizedOptions();
        final OptionSet optionSet = parser.parse(programArgs);
        return new DiscoveryData(optionSet.valueOf(gameDir), optionSet.valueOf(launchTarget), programArgs);
    }
}
