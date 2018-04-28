package cpw.mods.modlauncher;

import org.apache.logging.log4j.*;
import org.apache.logging.log4j.core.config.*;

public class Logging {
    static final Logger launcherLog = LogManager.getLogger("Launcher");
    static final Marker MODLAUNCHER = MarkerManager.getMarker("MODLAUNCHER");
    static final Marker CLASSLOADING = MarkerManager.getMarker("CLASSLOADING").addParents(MODLAUNCHER);
    static final Marker LAUNCHPLUGIN = MarkerManager.getMarker("LAUNCHPLUGIN").addParents(MODLAUNCHER);
}
