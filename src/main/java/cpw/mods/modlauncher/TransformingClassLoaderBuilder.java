package cpw.mods.modlauncher;

import cpw.mods.modlauncher.api.ITransformingClassLoaderBuilder;

import java.net.URL;
import java.net.URLConnection;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import java.util.jar.Manifest;

import static cpw.mods.modlauncher.api.LamdbaExceptionUtils.rethrowFunction;

class TransformingClassLoaderBuilder implements ITransformingClassLoaderBuilder {
    private List<Path> transformationPaths = new ArrayList<>();
    private Function<String, Optional<URL>> classBytesLocator;
    private Function<URLConnection, Optional<Manifest>> manifestLocator;

    URL[] getSpecialJarsAsURLs() {
        return transformationPaths.stream().map(rethrowFunction(path->path.toUri().toURL())).toArray(URL[]::new);
    }

    Function<String, Optional<URL>> getClassBytesLocator() {
        return classBytesLocator;
    }

    Function<URLConnection, Optional<Manifest>> getManifestLocator() {
        return manifestLocator;
    }

    @Override
    public void addTransformationPath(final Path path) {
        transformationPaths.add(path);
    }

    @Override
    public void setClassBytesLocator(final Function<String, Optional<URL>> additionalClassBytesLocator) {
        this.classBytesLocator = additionalClassBytesLocator;
    }

    @Override
    public void setManifestLocator(final Function<URLConnection, Optional<Manifest>> manifestLocator) {
        this.manifestLocator = manifestLocator;
    }
}
