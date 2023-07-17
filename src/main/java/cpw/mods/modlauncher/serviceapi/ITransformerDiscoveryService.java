/*
 * ModLauncher - for launching Java programs with in-flight transformation ability.
 *
 *     Copyright (C) 2017-2021 cpw
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

package cpw.mods.modlauncher.serviceapi;

import cpw.mods.jarhandling.SecureJar;

import java.nio.file.Path;
import java.util.List;

/**
 * Called early in setup, to allow pluggable "discovery" of additional transformer services.
 * FML uses this to identify transformers in the mods directory (e.g. Optifine) for loading into ModLauncher.
 */
public interface ITransformerDiscoveryService {
    /**
     * Return a list of additional paths to be added to transformer service discovery during loading.
     * @param gameDirectory The root game directory
     * @return The list of services
     */
    List<SecureJar> candidates(final Path gameDirectory);

    /**
     * Return a list of additional paths to be added to transformer service discovery during loading.
     *
     * Defaults to calling {@link #candidates(Path)}
     *
     * @param gameDirectory The root game directory
     * @param launchTarget The launch target
     * @return The list of services
     */
    default List<SecureJar> candidates(final Path gameDirectory, final String launchTarget) {
        return candidates(gameDirectory);
    }

    /**
     * An opportunity for early transformer services to do something once the service layer has
     * been built.
     *
     * @param launchTarget The launch target
     * @param arguments The full command arguments to the game
     */
    default void earlyInitialization(final String launchTarget, final String[] arguments) {

    }
}
