package cpw.mods.modlauncher.api;

import java.net.URL;
import java.net.URLConnection;
import java.nio.file.Path;
import java.util.Optional;
import java.util.function.Function;
import java.util.jar.Manifest;

public interface ITransformingClassLoaderBuilder {
    void addTransformationPath(Path path);

    void setClassBytesLocator(Function<String, Optional<URL>> additionalClassBytesLocator);

    void setManifestLocator(Function<URLConnection, Optional<Manifest>> manifestLocator);
}
