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

package cpw.mods.modlauncher.api.accesstransformer;

import org.objectweb.asm.Opcodes;

public enum AccessVisibilityModifier {
    KEEP(0), PROTECTED(1), PUBLIC(2);

    public final int sortingIndex;
    AccessVisibilityModifier(int sortingIndex)
    {
        this.sortingIndex = sortingIndex;
    }

    public boolean shouldBeReplacedBy(AccessVisibilityModifier other) {
        return (this.sortingIndex - other.sortingIndex) < 0;
    }

    public int getOpcode() {
        switch (this.sortingIndex) {
            case 0:
                throw new IllegalArgumentException("Can't get Opcode for KEEP!");
            case 1:
                return Opcodes.ACC_PROTECTED;
            case 2:
                return Opcodes.ACC_PUBLIC;
            default:
                throw new RuntimeException(String.format("Found invalid sorting index %d, but how is this possible?!", sortingIndex));
        }
    }
}
