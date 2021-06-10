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

package cpw.mods.modlauncher;

import cpw.mods.modlauncher.api.NamedPath;

import java.lang.module.Configuration;
import java.lang.module.ModuleFinder;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.function.BiFunction;
import java.util.function.Function;

public final class ModuleLayerHandler {
    enum Layer {
        BOOT(null),
        SERVICE(BOOT),
        PLUGIN(BOOT),
        GAME(SERVICE);

        private final Layer parent;

        Layer(final Layer parent) {
            this.parent = parent;
        }
    }
    private final EnumMap<Layer, List<NamedPath>> layers = new EnumMap<>(Layer.class);
    private final EnumMap<Layer, ModuleLayer> completedLayers = new EnumMap<>(Layer.class);

    ModuleLayerHandler() {
        completedLayers.put(Layer.BOOT, getClass().getModule().getLayer());
    }

    void addToLayer(final Layer layer, final NamedPath namedPath) {
        if (completedLayers.containsKey(layer)) throw new IllegalStateException("Layer already populated");
        layers.computeIfAbsent(layer, l->new ArrayList<>()).add(namedPath);
    }

    public ModuleLayer buildLayer(final Layer layer, final Function<NamedPath, ModuleFinder> finder, final BiFunction<String, Configuration, ClassLoader> classLoaderFinder) {
        final var parentLayer = completedLayers.get(layer.parent);
        final var finders = layers.getOrDefault(layer, List.of()).stream()
                .map(finder)
                .toArray(ModuleFinder[]::new);
        final var newConf = parentLayer.configuration().resolveAndBind(ModuleFinder.compose(finders), ModuleFinder.of(), List.of());
        final var modController = ModuleLayer.defineModules(newConf, List.of(parentLayer), f -> classLoaderFinder.apply(f, newConf));
        completedLayers.put(layer, modController.layer());
        return modController.layer();
    }
    public ModuleLayer getLayer(final Layer layer) {
        return completedLayers.get(layer);
    }

}
