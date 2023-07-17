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
import cpw.mods.modlauncher.api.IModuleLayerManager;

import java.lang.module.Configuration;
import java.lang.module.ModuleFinder;
import java.util.*;
import java.util.function.BiFunction;
import java.util.function.Consumer;

public final class ModuleLayerHandler implements IModuleLayerManager {
    public record LayerInfo(ModuleLayer layer, ModuleClassLoader cl) {}

    private final EnumMap<Layer, List<SecureJar>> layers = new EnumMap<>(Layer.class);
    private final EnumMap<Layer, LayerInfo> completedLayers = new EnumMap<>(Layer.class);

    ModuleLayerHandler() {
        ClassLoader classLoader = getClass().getClassLoader();
        // Create a new ModuleClassLoader from the boot module layer if it doesn't exist already.
        // This allows us to launch without BootstrapLauncher.
        ModuleClassLoader cl = classLoader instanceof ModuleClassLoader moduleCl ? moduleCl
            : new ModuleClassLoader("BOOT", ModuleLayer.boot().configuration(), List.of());
        completedLayers.put(Layer.BOOT, new LayerInfo(getClass().getModule().getLayer(), cl));
    }

    void addToLayer(final Layer layer, final SecureJar jar) {
        if (completedLayers.containsKey(layer)) throw new IllegalStateException("Layer already populated");
        layers.computeIfAbsent(layer, l -> new ArrayList<>()).add(jar);
    }

    public LayerInfo buildLayer(final Layer layer, BiFunction<Configuration, List<ModuleLayer>, ModuleClassLoader> classLoaderSupplier) {
        final var finder = layers.getOrDefault(layer, List.of())
                .toArray(SecureJar[]::new);
        final var targets = Arrays.stream(finder).map(SecureJar::name).toList();
        final var newConf = Configuration.resolveAndBind(JarModuleFinder.of(finder), Arrays.stream(layer.getParent()).map(completedLayers::get).map(li->li.layer().configuration()).toList(), ModuleFinder.of(), targets);
        final var allParents = Arrays.stream(layer.getParent()).map(completedLayers::get).map(LayerInfo::layer).<ModuleLayer>mapMulti((moduleLayer, comp)-> {
            comp.accept(moduleLayer);
            moduleLayer.parents().forEach(comp);
        }).toList();
        final var classLoader = classLoaderSupplier.apply(newConf, allParents);
        final var modController = ModuleLayer.defineModules(newConf, Arrays.stream(layer.getParent()).map(completedLayers::get).map(LayerInfo::layer).toList(), f->classLoader);
        completedLayers.put(layer, new LayerInfo(modController.layer(), classLoader));
        classLoader.setFallbackClassLoader(completedLayers.get(Layer.BOOT).cl());
        return new LayerInfo(modController.layer(), classLoader);
    }
    public LayerInfo buildLayer(final Layer layer) {
        return buildLayer(layer, (cf, p) -> new ModuleClassLoader("LAYER "+layer.name(), cf, p));
    }

    @Override
    public Optional<ModuleLayer> getLayer(final Layer layer) {
        return Optional.ofNullable(completedLayers.get(layer)).map(LayerInfo::layer);
    }

    public void updateLayer(Layer layer, Consumer<LayerInfo> action) {
        action.accept(completedLayers.get(layer));
    }
}
