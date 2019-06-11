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

package cpw.mods.modlauncher.api;

import java.util.*;
import java.util.function.*;

/**
 * Expose known naming domains into the system, to allow for modules to lookup alternative namings.
 *
 * mojang, srg and mcp will be available in certain environments.
 */
public interface INameMappingService {
    /**
     * The name of this namemapping.
     *
     * E.G. srgtomcp
     *
     * @return a unique name for this mapping
     */
    String mappingName();

    /**
     * A version number for this namemapping.
     *
     * @return a version number for this mapping
     */
    String mappingVersion();

    /**
     * The source and target you support. If your target is not the active naming, you will be ignored.
     *
     * @return A key (source naming) value (target naming) pair representing your source to target translation.
     */
    Map.Entry<String,String> understanding();

    /**
     * A function mapping a name to another name, for the given domain.
     *
     * The input string will be the name in the source naming, you should return the name in the target naming.
     *
     * @return A function mapping names
     */
    BiFunction<Domain, String, String> namingFunction();

    enum Domain { CLASS, METHOD, FIELD }
}
