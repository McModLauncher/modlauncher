package cpw.mods.modlauncher.serviceapi;

import java.nio.file.Path;
import java.util.List;

public interface ITransformerDiscoveryService {
    List<Path> candidates(final Path gameDirectory);
}
