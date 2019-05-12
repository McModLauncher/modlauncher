package cpw.mods.modlauncher.serviceapi;

import java.nio.file.Path;
import java.util.List;

/**
 * Called early in setup, to allow pluggable "discovery" of additional transformer services.
 * FML uses this to identify transformers in the mods directory (e.g. Optifine) for loading into ModLauncher.
 */
public interface ITransformerDiscoveryService {
    /**
     * Return a list of additional paths to be added to transformer service discovery during loading.
     * @param gameDirectory The root game directory
     * @return The list of services
     */
    List<Path> candidates(final Path gameDirectory);
}
