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

import org.apache.logging.log4j.*;

class LogMarkers {
    static final Marker MODLAUNCHER = MarkerManager.getMarker("MODLAUNCHER");
    static final Marker CLASSLOADING = MarkerManager.getMarker("CLASSLOADING").addParents(MODLAUNCHER);
    static final Marker LAUNCHPLUGIN = MarkerManager.getMarker("LAUNCHPLUGIN").addParents(MODLAUNCHER);
}
