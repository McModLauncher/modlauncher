package cpw.mods.modlauncher;

import cpw.mods.modlauncher.api.ITransformationService;
import cpw.mods.modlauncher.api.ITransformer;
import cpw.mods.modlauncher.api.ITransformerVotingContext;
import cpw.mods.modlauncher.api.TransformerVoteResult;

import javax.annotation.Nonnull;
import java.util.Set;

public class TransformerHolder<T> implements ITransformer<T> {
    private final ITransformer<T> wrapped;
    private final ITransformationService owner;

    public TransformerHolder(final ITransformer<T> wrapped, ITransformationService owner) {
        this.wrapped = wrapped;
        this.owner = owner;
    }

    @Nonnull
    @Override
    public T transform(final T input, final ITransformerVotingContext context) {
        return wrapped.transform(input, context);
    }

    @Nonnull
    @Override
    public TransformerVoteResult castVote(final ITransformerVotingContext context) {
        return wrapped.castVote(context);
    }

    @Nonnull
    @Override
    public Set<Target> targets() {
        return wrapped.targets();
    }

    @Override
    public String[] labels() {
        return wrapped.labels();
    }

    public ITransformationService owner() {
        return owner;
    }
}
