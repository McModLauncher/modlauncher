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
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import org.jetbrains.annotations.Nullable;
import java.net.URL;
import java.util.*;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.*;

import static cpw.mods.modlauncher.LogMarkers.*;

/**
 * Decorates {@link cpw.mods.modlauncher.api.ITransformationService} to track state and other runtime metadata.
 */
public class TransformationServiceDecorator {
    private static final Logger LOGGER = LogManager.getLogger();
    private final ITransformationService service;
    private boolean isValid;
    private static Set<String> classPrefixes = new HashSet<>();
    private static Set<String> resourceNames = new HashSet<>();

    TransformationServiceDecorator(ITransformationService service) {
        this.service = service;
    }

    void onLoad(IEnvironment env, Set<String> otherServices) {
        try {
            LOGGER.debug(MODLAUNCHER,"Loading service {}", this.service::name);
            this.service.onLoad(env, otherServices);
            this.isValid = true;
            LOGGER.debug(MODLAUNCHER,"Loaded service {}", this.service::name);
        } catch (IncompatibleEnvironmentException e) {
            LOGGER.error(MODLAUNCHER,"Service failed to load {}", this.service.name(), e);
            this.isValid = false;
        }
    }

    boolean isValid() {
        return isValid;
    }

    void onInitialize(IEnvironment environment) {
        LOGGER.debug(MODLAUNCHER,"Initializing transformation service {}", this.service::name);
        this.service.initialize(environment);
        LOGGER.debug(MODLAUNCHER,"Initialized transformation service {}", this.service::name);
    }

    public void gatherTransformers(TransformStore transformStore) {
        LOGGER.debug(MODLAUNCHER,"Initializing transformers for transformation service {}", this.service::name);
        final List<? extends ITransformer<?>> transformers = this.service.transformers();
        Objects.requireNonNull(transformers, "The transformers list should not be null");
        transformers.forEach(xform -> {
            final TargetType<?> targetType = xform.getTargetType();
            Objects.requireNonNull(targetType, "Transformer type must not be null");
            final Set<? extends ITransformer.Target<?>> targets = xform.targets();
            if (!targets.isEmpty()) {
                final Map<TargetType<?>, List<TransformTargetLabel>> targetTypeListMap = targets.stream()
                    .map(TransformTargetLabel::new)
                    .collect(Collectors.groupingBy(TransformTargetLabel::getTargetType));
                if (targetTypeListMap.keySet().size() > 1 || !targetTypeListMap.containsKey(targetType)) {
                    LOGGER.error(MODLAUNCHER,"Invalid target {} for transformer {}", targetType, xform);
                    throw new IllegalArgumentException("The transformer contains invalid targets");
                }
                targetTypeListMap.values()
                    .stream()
                    .flatMap(Collection::stream)
                    .forEach(target -> transformStore.addTransformer(target, xform, service));
            }
        });
        LOGGER.debug(MODLAUNCHER,"Initialized transformers for transformation service {}", this.service::name);
    }

    ITransformationService getService() {
        return service;
    }

    List<ITransformationService.Resource> runScan(final Environment environment) {
        LOGGER.debug(MODLAUNCHER,"Beginning scan trigger - transformation service {}", this.service::name);
        final List<ITransformationService.Resource> scanResults = this.service.beginScanning(environment);
        LOGGER.debug(MODLAUNCHER,"End scan trigger - transformation service {}", this.service::name);
        return scanResults;
    }

    Function<String,Optional<URL>> getClassLoader() {
        final Map.Entry<Set<String>, Supplier<Function<String, Optional<URL>>>> classesLocator = this.service.additionalClassesLocator();
        if (classesLocator != null) {
            final HashSet<String> packagePrefixes = new HashSet<>(classesLocator.getKey());
            final Set<String> badPrefixes = packagePrefixes.stream().
                    filter(s ->
                            // No prefixes starting with net.minecraft.
                            s.startsWith("net.minecraft.") ||
                            // No prefixes starting with net.minecraftforge.
                            s.startsWith("net.minecraftforge.") ||
                            // No prefixes starting with net.neoforged.
                            s.startsWith("net.neoforged.") ||
                            // No prefixes already claimed
                            classPrefixes.contains(s) ||
                            // No prefixes not ending in a dot
                            !s.endsWith(".") ||
                            // No prefixes starting without a dot after the first character
                            s.indexOf('.') <= 0).
                    collect(Collectors.toSet());
            if (!badPrefixes.isEmpty()) {
                badPrefixes.forEach(s -> LOGGER.error("Illegal prefix specified for {} : {}", this.service.name(), s));
                throw new IllegalArgumentException("Bad prefixes specified");
            }
            classPrefixes.addAll(classesLocator.getKey());
        }

        final Map.Entry<Set<String>, Supplier<Function<String, Optional<URL>>>> resourcesLocator = this.service.additionalResourcesLocator();
        if (resourcesLocator!=null) {
            final HashSet<String> resNames = new HashSet<>(resourcesLocator.getKey());
            final Set<String> badResourceNames = resNames.stream().
                    filter(s -> s.endsWith(".class") || resourceNames.contains(s)).
                    collect(Collectors.toSet());
            if (!badResourceNames.isEmpty()) {
                badResourceNames.forEach(s -> LOGGER.error("Illegal resource name specified for {} : {}", this.service.name(), s));
                throw new IllegalArgumentException("Bad resources specified");
            }
            resourceNames.addAll(resourcesLocator.getKey());
        }
        return s -> getOptionalURL(classesLocator, resourcesLocator, s);
    }

    private Optional<URL> getOptionalURL(@Nullable Map.Entry<Set<String>, Supplier<Function<String, Optional<URL>>>> classes, @org.jetbrains.annotations.Nullable Map.Entry<Set<String>, Supplier<Function<String, Optional<URL>>>> resources, final String name) {
        if (classes != null && name.endsWith(".class")) {
            for (String pfx : classes.getKey()) {
                if (name.startsWith(pfx.replace('.','/'))) {
                    return classes.getValue().get().apply(name);
                }
            }
        } else if (resources != null && !name.endsWith(".class")) {
            for (String pfx : resources.getKey()) {
                if (Objects.equals(name, pfx)) {
                    return resources.getValue().get().apply(name);
                }
            }
        }
        return Optional.empty();
    }

    public List<ITransformationService.Resource> onCompleteScan(IModuleLayerManager moduleLayerManager) {
        return this.service.completeScan(moduleLayerManager);
    }
}
