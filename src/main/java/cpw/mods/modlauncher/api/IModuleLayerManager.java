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

package cpw.mods.modlauncher.api;

import java.util.Optional;

public interface IModuleLayerManager {
    Optional<ModuleLayer> getLayer(Layer layer);

    enum Layer {
        BOOT(),
        SERVICE(BOOT),
        PLUGIN(BOOT),
        GAME(PLUGIN, SERVICE);

        private final Layer[] parent;

        Layer(final Layer... parent) {
            this.parent = parent;
        }

        public Layer[] getParent() {
            return parent;
        }
    }
}
