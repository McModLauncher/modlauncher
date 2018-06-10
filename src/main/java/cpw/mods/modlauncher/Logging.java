package cpw.mods.modlauncher;

import org.apache.logging.log4j.*;

public class Logging {
    static final Marker MODLAUNCHER = MarkerManager.getMarker("MODLAUNCHER");
    static final Marker CLASSLOADING = MarkerManager.getMarker("CLASSLOADING").addParents(MODLAUNCHER);
    static final Marker LAUNCHPLUGIN = MarkerManager.getMarker("LAUNCHPLUGIN").addParents(MODLAUNCHER);
}
