package cpw.mods.modlauncher.api;

/**
 * Indicate an incompatible environment to the modlauncher system.
 */
public class IncompatibleEnvironmentException extends Exception {
    public IncompatibleEnvironmentException(String message) {
        super(message);
    }
}
