package cpw.mods.modlauncher;

import cpw.mods.modlauncher.api.ITransformer;
import cpw.mods.modlauncher.api.VoteResult;

/**
 * A vote from a particular transformer
 */
@SuppressWarnings("WeakerAccess")
public class Vote<T>
{
    private final ITransformer<T> transformer;
    private final VoteResult result;

    public Vote(VoteResult vr, ITransformer<T> transformer)
    {
        this.transformer = transformer;
        this.result = vr;
    }

    public VoteResult getResult()
    {
        return result;
    }

    public ITransformer<T> getTransformer()
    {
        return transformer;
    }
}
