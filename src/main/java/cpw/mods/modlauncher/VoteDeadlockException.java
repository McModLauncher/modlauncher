package cpw.mods.modlauncher;

import java.util.List;

class VoteDeadlockException extends RuntimeException
{
    <T> VoteDeadlockException(List<TransformerVote<T>> votes, Class<?> aClass)
    {
    }
}
