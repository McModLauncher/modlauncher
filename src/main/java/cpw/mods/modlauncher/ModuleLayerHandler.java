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

import cpw.mods.cl.JarModuleFinder;
import cpw.mods.cl.ModuleClassLoader;
import cpw.mods.jarhandling.SecureJar;
import cpw.mods.modlauncher.api.NamedPath;

import java.lang.module.ModuleFinder;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;

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

    record LayerInfo(ModuleLayer layer, ModuleClassLoader cl) {}
    public LayerInfo buildLayer(final Layer layer) {
        final var parentLayer = completedLayers.get(layer.parent);
        final var finder = layers.getOrDefault(layer, List.of()).stream()
                .map(np-> SecureJar.from(np.paths()))
                .toArray(SecureJar[]::new);
        final var newConf = parentLayer.configuration().resolveAndBind(JarModuleFinder.of(finder), ModuleFinder.of(), List.of());
        final var classLoader = new ModuleClassLoader("LAYER "+layer.name(), newConf, List.of(parentLayer));
        final var modController = ModuleLayer.defineModules(newConf, List.of(parentLayer), f->classLoader);
        completedLayers.put(layer, modController.layer());
        return new LayerInfo(modController.layer(), classLoader);
    }
    public ModuleLayer getLayer(final Layer layer) {
        return completedLayers.get(layer);
    }

}
