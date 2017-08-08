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

import javax.annotation.Nullable;
import java.util.Arrays;
import java.util.List;

/**
 * A class that allows easy transformation of access flags.
 * The AT with the highest access wins.
 * <br>
 * Example:
 * There are two ATs that transform the same method: One wants protected access, the other one public access. In this case,
 * the method will become public to avoid unexpected behavior
 */
public class AccessTransformation {
    private static final List<Integer> validOpcodes = Arrays.asList(Opcodes.ACC_PROTECTED, Opcodes.ACC_PUBLIC);

    public final int newAccessFlag;
    public final String className;
    public final AccessTransformationTarget type;
    @Nullable
    public final String[] names;

    private AccessTransformation(int newAccessFlag, String className, AccessTransformationTarget type, @Nullable String[] names) {
        this.names = names;
        if (!validOpcodes.contains(newAccessFlag))
            throw new IllegalArgumentException(String.format("Invalid access flag %d for AT transforming %s access level at class %s!", newAccessFlag, type, className));
        this.newAccessFlag = newAccessFlag;
        this.className = className;
        this.type = type;
    }

    /**
     * Constructs an AccessTransformer that transforms the access of a class.
     * Only the access of the class will be transformed, all methods and fields inside will be ignored and need a separate AT.
     * @param newAccessFlag The new access flag, either {@code Opcodes.ACC_PROTECTED} or {@code Opcodes.ACC_PUBLIC}.
     * @param className The name of the class to transform. Note that this needs to be the name <b>AFTER</b> the {@link cpw.mods.modlauncher.api.INameMappingService} did run.
     */
    public static AccessTransformation createClassTransformer(int newAccessFlag, String className) {
        return new AccessTransformation(newAccessFlag, className, AccessTransformationTarget.CLASS, null);
    }

    /**
     * Constructs an AccessTransformer that transforms the access of a field.
     * All fields will have the same specified access flag. If you want different access flags, you have to create multiple ATs
     * <br>
     * NOTE this runs <b>AFTER</b> the {@link cpw.mods.modlauncher.api.INameMappingService} did run, make sure to specify the transformed names
     * @param newAccessFlag The new access flag, either {@code Opcodes.ACC_PROTECTED} or {@code Opcodes.ACC_PUBLIC}.
     * @param className The name of the class in which the specified fields are.
     */
    public static AccessTransformation createFieldTransformer(int newAccessFlag, String className, String... fieldNames) {
        return new AccessTransformation(newAccessFlag, className, AccessTransformationTarget.FIELD, fieldNames);
    }

    /**
     * Constructs an AccessTransformer that transforms the access of a method.
     * All methods will have the same specified access flag. If you want different access flags, you have to create multiple ATs
     * <br>
     * NOTE this runs <b>AFTER</b> the {@link cpw.mods.modlauncher.api.INameMappingService} did run, make sure to specify the transformed names
     * @param newAccessFlag The new access flag, either {@code Opcodes.ACC_PROTECTED} or {@code Opcodes.ACC_PUBLIC}.
     * @param className The name of the class in which the specified methods are.
     * @param methodNames The names of the methods to change access.
     */
    public static AccessTransformation createMethodTransformer(int newAccessFlag, String className, String... methodNames) {
        return new AccessTransformation(newAccessFlag, className, AccessTransformationTarget.METHOD, methodNames);
    }
}
