/*
 * ModLauncher - for launching Java programs with in-flight transformation ability.
 *
 *     Copyright (C) 2017-2019 cpw
 *
 *     This program is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU Lesser General Public License as published by
 *     the Free Software Foundation, version 3 of the License.
 *
 *     This program is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU Lesser General Public License for more details.
 *
 *     You should have received a copy of the GNU Lesser General Public License
 *     along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package cpw.mods.modlauncher;

import cpw.mods.modlauncher.api.*;

import org.jetbrains.annotations.NotNull;
import java.util.Set;

public class TransformerHolder<T> implements ITransformer<T> {
    private final ITransformer<T> wrapped;
    private final ITransformationService owner;

    public TransformerHolder(final ITransformer<T> wrapped, ITransformationService owner) {
        this.wrapped = wrapped;
        this.owner = owner;
    }

    @NotNull
    @Override
    public T transform(final T input, final ITransformerVotingContext context) {
        return wrapped.transform(input, context);
    }

    @NotNull
    @Override
    public TransformerVoteResult castVote(final ITransformerVotingContext context) {
        return wrapped.castVote(context);
    }

    @NotNull
    @Override
    public Set<Target<T>> targets() {
        return wrapped.targets();
    }

    @Override
    public TargetType<T> getTargetType() {
        return wrapped.getTargetType();
    }

    @Override
    public String[] labels() {
        return wrapped.labels();
    }

    public ITransformationService owner() {
        return owner;
    }
}
