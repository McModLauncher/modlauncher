package cpw.mods.modlauncher;

import java.util.List;

/**
 * Exception thrown when a voter rejects the entire configuration
 */
@SuppressWarnings("WeakerAccess")
public class VoteRejectedException extends RuntimeException
{
    public <T> VoteRejectedException(List<Vote<T>> votes, Class<?> aClass)
    {
    }
}
