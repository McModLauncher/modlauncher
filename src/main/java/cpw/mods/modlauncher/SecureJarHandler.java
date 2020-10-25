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

import cpw.mods.modlauncher.api.LamdbaExceptionUtils;
import sun.security.util.ManifestEntryVerifier;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.net.URL;
import java.security.*;
import java.util.HashMap;
import java.util.Map;
import java.util.jar.JarEntry;
import java.util.jar.Manifest;

public class SecureJarHandler {
    private static final Class<?> JVCLASS = LamdbaExceptionUtils.uncheck(()->Class.forName("java.util.jar.JarVerifier"));
    private static final Method BEGIN_ENTRY = LamdbaExceptionUtils.uncheck(()->JVCLASS.getMethod("beginEntry", JarEntry.class, ManifestEntryVerifier.class));
    private static final Method UPDATE = LamdbaExceptionUtils.uncheck(()->JVCLASS.getMethod("update", int.class, byte[].class, int.class, int.class, ManifestEntryVerifier.class));
    private static final Field JV = LamdbaExceptionUtils.uncheck(()->Manifest.class.getDeclaredField("jv"));
    static {
        JV.setAccessible(true);
        BEGIN_ENTRY.setAccessible(true);
        UPDATE.setAccessible(true);
    }
    @SuppressWarnings("ConstantConditions")
    public static CodeSource getSigners(final String name, @Nullable final URL url, final byte[] bytes, @Nullable final Manifest manifest) {
        if (manifest == null) return null;
        if (url == null) return null;
        JarEntry je = new JarEntry(name);
        ManifestEntryVerifier mev = new ManifestEntryVerifier(manifest);
        Object obj = LamdbaExceptionUtils.uncheck(()->JV.get(manifest));
        // begin Entry on JarVerifier
        LamdbaExceptionUtils.uncheck(()->BEGIN_ENTRY.invoke(obj, je, mev));
        // Feed the bytes to the underlying MEV
        LamdbaExceptionUtils.uncheck(()->UPDATE.invoke(obj, bytes.length, bytes, 0, bytes.length, mev));
        // Generate the cert check - signers will be loaded into the dummy jar entry
        LamdbaExceptionUtils.uncheck(()->UPDATE.invoke(obj, -1, bytes, 0, bytes.length, mev));
        CodeSigner[] signers = je.getCodeSigners();
        return new CodeSource(url, signers);
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
}
