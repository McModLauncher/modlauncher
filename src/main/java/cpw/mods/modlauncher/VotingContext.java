package cpw.mods.modlauncher;

import cpw.mods.modlauncher.api.*;

/**
 * The internal vote context structure.
 */
public class VotingContext implements ITransformerVotingContext {
    private final String className;
    private final boolean classExists;
    private final byte[] sha256;

    public VotingContext(String className, boolean classExists, byte[] sha256sum) {
        this.className = className;
        this.classExists = classExists;
        this.sha256 = sha256sum;
    }
}
