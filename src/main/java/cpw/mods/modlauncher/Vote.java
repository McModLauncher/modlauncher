package cpw.mods.modlauncher;

import cpw.mods.modlauncher.api.Transformer;
import cpw.mods.modlauncher.api.VoteResult;

/**
 * A vote from a particular transformer
 */
@SuppressWarnings("WeakerAccess")
public class Vote<T>
{
    private final Transformer<T> transformer;
    private final VoteResult result;

    public Vote(VoteResult vr, Transformer<T> transformer)
    {
        this.transformer = transformer;
        this.result = vr;
    }

    public VoteResult getResult()
    {
        return result;
    }

    public Transformer<T> getTransformer()
    {
        return transformer;
    }
}
