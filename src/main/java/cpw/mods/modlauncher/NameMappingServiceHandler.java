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

import java.util.*;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.stream.Collectors;

import static cpw.mods.modlauncher.LogMarkers.*;

/**
 * Allow names to be transformed between naming domains.
 */
class NameMappingServiceHandler {
    private static final Logger LOGGER = LogManager.getLogger();
    private final ServiceLoader<INameMappingService> namingServices;
    private final Map<String, NameMappingServiceDecorator> namingTable;
    private Map<String, NameMappingServiceDecorator> nameBindings;

    public NameMappingServiceHandler() {
        namingServices = ServiceLoaderStreamUtils.errorHandlingServiceLoader(INameMappingService.class, serviceConfigurationError -> LOGGER.fatal("Encountered serious error loading naming service, expect problems", serviceConfigurationError));
        namingTable = ServiceLoaderStreamUtils.toMap(namingServices, INameMappingService::mappingName, NameMappingServiceDecorator::new);
        LOGGER.debug(MODLAUNCHER,"Found naming services : [{}]", () -> String.join(",", namingTable.keySet()));
    }


    public Optional<BiFunction<INameMappingService.Domain,String,String>> findNameTranslator(final String targetNaming) {
        return Optional.ofNullable(nameBindings.get(targetNaming)).map(NameMappingServiceDecorator::function);
    }

    public void bindNamingServices(final String currentNaming) {
        LOGGER.debug(MODLAUNCHER, "Current naming domain is '{}'", currentNaming);
        nameBindings = namingTable.values().stream().
                filter(nameMappingServiceDecorator -> nameMappingServiceDecorator.validTarget(currentNaming)).
                collect(Collectors.toMap(NameMappingServiceDecorator::understands, Function.identity()));
        LOGGER.debug(MODLAUNCHER, "Identified name mapping providers {}", nameBindings);
    }
}
