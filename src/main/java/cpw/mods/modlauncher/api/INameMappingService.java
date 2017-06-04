/*
 * Modlauncher - utility to launch Minecraft-like game environments with runtime transformation
 * Copyright ©2016-2017 cpw and others
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301  USA
 */

package cpw.mods.modlauncher.api;

import java.util.Set;
import java.util.function.Function;

/**
 * Expose known naming domains into the system, to allow for modules to
 * lookup alternative namings.
 * notch namemappings will always be available. srg and mcp will be available in certain environments.
 */
public interface INameMappingService
{
    /**
     * The name of this namemapping.
     *
     * @return a unique name for this mapping
     */
    String mappingName();

    /**
     * A version number for this namemapping.

     * @return a version number for this mapping
     */
    String mappingVersion();

    /**
     * The set of mapping targets this namemapping understands.
     * Trivially, you should understand yourself.
     * @return A set of other mapping targets you can translate to.
     */
    Set<String> understanding();

    /**
     * A function mapping a name to another name, for the given domain
     * and naming target.
     * @param domain The naming domain
     * @param target The target from {@link #understanding} for which
     *                we wish to map the names
     * @return A function mapping names
     */
    Function<String, String> namingFunction(Domain domain, String target);

    enum Domain { CLASS, METHOD, FIELD }
}
