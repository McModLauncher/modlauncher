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
import org.jetbrains.annotations.VisibleForTesting;

import java.util.*;

import static cpw.mods.modlauncher.LogMarkers.*;

/**
 * Decorates {@link cpw.mods.modlauncher.api.ITransformationService} to track state and other runtime metadata.
 */
public class TransformationServiceDecorator {
    private static final Logger LOGGER = LogManager.getLogger();
    private final ITransformationService service;
    private boolean isValid;

    @VisibleForTesting
    public TransformationServiceDecorator(ITransformationService service) {
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
        for (ITransformer<?> xform : transformers) {
            transformStore.addTransformer(xform, service);
        }
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

    public List<ITransformationService.Resource> onCompleteScan(IModuleLayerManager moduleLayerManager) {
        return this.service.completeScan(moduleLayerManager);
    }
}
