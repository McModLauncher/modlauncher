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

import java.util.Objects;
import java.util.function.BiFunction;

/**
 * Decorator for Naming Services
 */
class NameMappingServiceDecorator {
    private final INameMappingService service;

    public NameMappingServiceDecorator(INameMappingService service) {
        this.service = service;
    }

    public boolean validTarget(final String origin) {
        return Objects.equals(this.service.understanding().getValue(), origin);
    }
    public String understands() {
        return this.service.understanding().getKey();
    }

    public BiFunction<INameMappingService.Domain, String, String> function() {
        return this.service.namingFunction();
    }

    public String toString() {
        return this.service.mappingName() + ":" + this.service.mappingVersion();
    }
}
