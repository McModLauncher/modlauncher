package cpw.mods.modlauncher.api;

import java.util.jar.JarEntry;
import java.util.jar.Manifest;

public final class JarEntryWithManifest {

    private final JarEntry jarEntry;
    private final Manifest manifest;

    public JarEntryWithManifest(JarEntry jarEntry, Manifest manifest) {
        this.jarEntry = jarEntry;
        this.manifest = manifest;
    }

    public JarEntry getJarEntry() {
        return jarEntry;
    }

    public Manifest getManifest() {
        return manifest;
    }
}
