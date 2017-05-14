package cpw.mods.modlauncher;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.config.Configurator;

/**
 * Logging holder
 */
@SuppressWarnings("WeakerAccess")
public class Logging
{
    static
    {
        Configurator.setRootLevel(Level.DEBUG);
    }

    static final Logger launcherLog = LogManager.getLogger("Launcher");
}
