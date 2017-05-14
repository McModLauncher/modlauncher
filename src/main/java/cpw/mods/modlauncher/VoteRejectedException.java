package cpw.mods.modlauncher;

import java.util.List;

class VoteRejectedException extends RuntimeException
{
    <T> VoteRejectedException(List<TransformerVote<T>> votes, Class<?> aClass)
    {
    }
}
