package cpw.mods.modlauncher;

import org.apache.logging.log4j.*;
import org.apache.logging.log4j.core.config.*;

public class Logging {
    static final Logger launcherLog = LogManager.getLogger("Launcher");
    static final Marker CLASSLOADING = MarkerManager.getMarker("CLASSLOADING");

    static {
        Configurator.setRootLevel(Level.INFO);
    }
}
