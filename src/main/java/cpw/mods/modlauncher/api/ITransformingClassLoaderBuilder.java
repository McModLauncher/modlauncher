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

import java.net.URL;
import java.net.URLConnection;
import java.nio.file.Path;
import java.util.Optional;
import java.util.function.Function;
import java.util.jar.Manifest;

public interface ITransformingClassLoaderBuilder {
    void addTransformationPath(Path path);

    void setClassBytesLocator(Function<String, Optional<URL>> additionalClassBytesLocator);

    void setManifestLocator(Function<URLConnection, Optional<Manifest>> manifestLocator);
}
