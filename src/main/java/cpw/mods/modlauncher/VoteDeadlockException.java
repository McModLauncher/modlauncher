package cpw.mods.modlauncher;

import java.util.List;

/**
 * Exception thrown when a vote impass occurs
 */
@SuppressWarnings("WeakerAccess")
public class VoteDeadlockException extends RuntimeException
{
    <T> VoteDeadlockException(List<TransformerVote<T>> votes, Class<?> aClass)
    {
    }
}
