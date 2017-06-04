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

import java.io.File;
import java.util.Optional;
import java.util.function.Supplier;

/**
 * System environment. Global properties relevant to the current environment.
 */
public interface IEnvironment
{
    final class Keys
    {
        public static final Supplier<TypesafeMap.Key<String>> VERSION = new TypesafeMap.KeyBuilder<>("version", String.class, IEnvironment.class);
        public static final Supplier<TypesafeMap.Key<File>> GAMEDIR = new TypesafeMap.KeyBuilder<>("gamedir", File.class, IEnvironment.class);
        public static final Supplier<TypesafeMap.Key<File>> ASSETSDIR = new TypesafeMap.KeyBuilder<>("assetsdir", File.class, IEnvironment.class);
    }

    <T> Optional<T> getProperty(TypesafeMap.Key<T> key);
}
