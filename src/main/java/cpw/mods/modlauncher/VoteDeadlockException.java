package cpw.mods.modlauncher;

import java.util.*;

/**
 * Exception thrown when a vote impass occurs
 */
public class VoteDeadlockException extends RuntimeException {
    <T> VoteDeadlockException(List<TransformerVote<T>> votes, Class<?> aClass) {
    }
}
