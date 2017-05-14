package cpw.mods.modlauncher;

import java.util.List;

/**
 * Exception thrown when a vote impass occurs
 */
@SuppressWarnings("WeakerAccess")
public class VoteDeadlockException extends RuntimeException
{
    public <T> VoteDeadlockException(List<Vote<T>> votes, Class<?> aClass)
    {
    }
}
