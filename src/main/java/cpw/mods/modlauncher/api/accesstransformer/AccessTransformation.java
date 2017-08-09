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

import cpw.mods.modlauncher.TransformTargetLabel;

/**
 * A class that allows easy transformation of access flags.
 * ATs get run before the transformers are called to transform the class/method/field
 * It is NOT possible to downgrade the access. F.E. if an AT wants protected but the class/field/method is already public, this will throw an exception
 * ATs will get merged if more ATs want to modify the same class/method/field. See some examples below.
 * <br>
 * Example:
 * There are two ATs that transform the same method:
 * <p>One wants protected access, the other one public access. In this case, the method will be public.</p>
 * <p>One wants final access, the other wants to keep. In this case, the method will keep the flag.</p>
 */
public class AccessTransformation {
    public AccessVisibilityModifier visibilityModifier;
    public AccessWriteModifier finalModifier;
    public final TransformTargetLabel label;

    /**
     * Constructs a new AT rule.
     * @param visibilityModifier The new visibility flag.
     * @param finalModifier The new write access flag.
     * @param label The target to transform.
     * @throws IllegalArgumentException If both the {@code AccessVisibilityModifier} and {@code AccessWriteModifier} are KEEP
     */
    public AccessTransformation(AccessVisibilityModifier visibilityModifier, AccessWriteModifier finalModifier, TransformTargetLabel label) throws IllegalArgumentException {
        this.label = label;
        if (visibilityModifier == AccessVisibilityModifier.KEEP && finalModifier == AccessWriteModifier.KEEP)
            throw new IllegalArgumentException("Both the visibilityModifier and the writeModifier are KEEP. This AT is useless!");
        this.finalModifier = finalModifier;
        this.visibilityModifier = visibilityModifier;
    }
}
