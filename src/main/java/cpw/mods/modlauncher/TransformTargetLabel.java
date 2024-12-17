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

import cpw.mods.modlauncher.api.*;
import org.objectweb.asm.*;
import org.objectweb.asm.tree.*;

import java.util.*;

import static cpw.mods.modlauncher.api.TargetType.*;

/**
 * Detailed targetting information
 */
public final class TransformTargetLabel {

    private final Type className;
    private final String elementName;
    private final Type elementDescriptor;
    private final TargetType<?> labelType;
    
    TransformTargetLabel(ITransformer.Target<?> target) {
        this(target.className(), target.elementName(), target.elementDescriptor(), target.targetType());
    }
    private TransformTargetLabel(String className, String elementName, String elementDescriptor, TargetType<?> labelType) {
        this.className = Type.getObjectType(className.replace('.', '/'));
        this.elementName = elementName;
        this.elementDescriptor = elementDescriptor.length() > 0 ? Type.getMethodType(elementDescriptor) : Type.VOID_TYPE;
        this.labelType = labelType;
    }
    public TransformTargetLabel(String className, String fieldName) {
        this(className, fieldName, "", FIELD);
    }

    TransformTargetLabel(String className, String methodName, String methodDesc) {
        this(className, methodName, methodDesc, METHOD);
    }

    @Deprecated
    public TransformTargetLabel(String className) {
        this(className, "", "", CLASS);
    }

    public TransformTargetLabel(String className, TargetType<ClassNode> type) {
        this(className, "", "", type);
    }

    final Type getClassName() {
        return this.className;
    }

    public final String getElementName() {
        return this.elementName;
    }

    public final Type getElementDescriptor() {
        return this.elementDescriptor;
    }

    public TargetType<?> getTargetType() {
        return this.labelType;
    }

    public int hashCode() {
        return Objects.hash(this.className, this.elementName, this.elementDescriptor);
    }

    @Override
    public boolean equals(Object obj) {
        try {
            TransformTargetLabel tl = (TransformTargetLabel) obj;
            return Objects.equals(this.className, tl.className)
                    && Objects.equals(this.elementName, tl.elementName)
                    && Objects.equals(this.elementDescriptor, tl.elementDescriptor);
        } catch (ClassCastException cce) {
            return false;
        }
    }

    @Override
    public String toString() {
        return "Target : " + Objects.toString(labelType) + " {" + Objects.toString(className) + "} {" + Objects.toString(elementName) + "} {" + Objects.toString(elementDescriptor) + "}";
    }
}
