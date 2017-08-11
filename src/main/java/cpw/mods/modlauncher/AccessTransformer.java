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

package cpw.mods.modlauncher;

import cpw.mods.modlauncher.api.accesstransformer.AccessTransformation;
import cpw.mods.modlauncher.api.accesstransformer.AccessVisibilityModifier;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldNode;
import org.objectweb.asm.tree.MethodNode;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

class AccessTransformer
{

    @Nonnull
    static FieldNode transform(@Nullable AccessTransformation fieldAT, FieldNode field)
    {
        field.access = getAccessForAT(field.access, fieldAT);
        return field;
    }

    @Nonnull
    static MethodNode transform(@Nullable AccessTransformation fieldAT, MethodNode method)
    {
        method.access = getAccessForAT(method.access, fieldAT);
        return method;
    }

    @Nonnull
    static ClassNode transform(@Nullable AccessTransformation fieldAT, ClassNode clazz)
    {
        clazz.access = getAccessForAT(clazz.access, fieldAT);
        return clazz;
    }

    private static int getAccessForAT(int oldAccess, @Nullable AccessTransformation at)
    {
        if (at == null)
            return oldAccess;
        int modifiedAccess = (oldAccess & ~7);
        if (at.visibilityModifier != AccessVisibilityModifier.KEEP)
        {
            int newAccess = at.visibilityModifier.getOpcode();
            switch (oldAccess & 7)
            {
                case Opcodes.ACC_PRIVATE:
                case 0:
                    modifiedAccess |= newAccess;
                    break;
                case Opcodes.ACC_PROTECTED:
                    if (newAccess == Opcodes.ACC_PUBLIC) //fine, just what we expect
                    {
                        modifiedAccess |= newAccess;
                    }
                    else
                    {
                        Logging.launcherLog.warn(Logging.ACCESS_TRANSFORMING, "Found unnecessary visibility AT for {}, as it is already protected!", at.label);
                    }
                    break;
                case Opcodes.ACC_PUBLIC:
                    if (newAccess != Opcodes.ACC_PUBLIC)
                    {
                        Logging.launcherLog.error(Logging.ACCESS_TRANSFORMING, "Invalid AT for field {}, access in AT is lower than in code, ignoring!", at.label);
                    }
                    else
                    {
                        Logging.launcherLog.warn(Logging.ACCESS_TRANSFORMING, "Found unnecessary visibility AT for {}, as it is already public!", at.label);
                    }
                    break;
                default: //No clue how this should happen though...
                    Logging.launcherLog.fatal(Logging.ACCESS_TRANSFORMING, "Found invalid Opcode {}, how could this happen?!", oldAccess & 7);
                    throw new RuntimeException("Found invalid Opcode " + (oldAccess & 7) + " while applying AT");
            }
        }
        switch (at.finalModifier)
        {
            case KEEP:
                //ignore
                break;
            case FINAL:
                modifiedAccess |= Opcodes.ACC_FINAL;
                break;
            case NONFINAL:
                modifiedAccess &= ~Opcodes.ACC_FINAL;
                break;
        }
        return modifiedAccess;
    }
}
