/*
 * ModLauncher - for launching Java programs with in-flight transformation ability.
 *
 *     Copyright (C) 2017-2020 cpw
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

import cpw.mods.modlauncher.api.JarEntryWithManifest;

import javax.annotation.Nullable;

import java.net.URL;
import java.security.*;
import java.util.HashMap;
import java.util.Map;
import java.util.jar.JarEntry;
import java.util.jar.Manifest;

public class SecureJarHandler {

    // Manifest is required to originate from a read JarEntry, otherwise it will not work
    // in our case the JarEntry will always be read fully before coming here, so getCodeSigners will be not null, if there are any
    public static CodeSource createCodeSource(@Nullable final URL url, @Nullable final JarEntryWithManifest entryWithManifest) {
        if (entryWithManifest == null) return null;
        if (url == null) return null;
        JarEntry je = entryWithManifest.getJarEntry();
        Manifest manifest = entryWithManifest.getManifest();
        if (je == null || manifest == null) return null;
        return new CodeSource(url, je.getCodeSigners());
    }

    private static final Map<CodeSource, ProtectionDomain> pdCache = new HashMap<>();
    public static ProtectionDomain createProtectionDomain(CodeSource codeSource, ClassLoader cl) {
        synchronized (pdCache) {
            return pdCache.computeIfAbsent(codeSource, cs->{
                Permissions perms = new Permissions();
                perms.add(new AllPermission());
                return new ProtectionDomain(codeSource, perms, cl, null);
            });
        }
    }

    public static boolean canHandleSecuredJars() {
        return true;
    }
}
