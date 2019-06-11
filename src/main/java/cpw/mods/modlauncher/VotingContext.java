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

import java.util.function.Supplier;

/**
 * The internal vote context structure.
 */
public class VotingContext implements ITransformerVotingContext {
    private final String className;
    private final boolean classExists;
    private final Supplier<byte[]> sha256;

    public VotingContext(String className, boolean classExists, Supplier<byte[]> sha256sum) {
        this.className = className;
        this.classExists = classExists;
        this.sha256 = sha256sum;
    }

    public String getClassName() {
        return className;
    }
}
