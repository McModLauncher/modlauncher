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
import java.util.function.*;

import static cpw.mods.modlauncher.TransformTargetLabel.LabelType.*;

/**
 * Detailed targetting information
 */
public final class TransformTargetLabel {

    private final Type className;
    private final String elementName;
    private final Type elementDescriptor;
    private final LabelType labelType;
    TransformTargetLabel(ITransformer.Target target) {
        this(target.getClassName(), target.getElementName(), target.getElementDescriptor(), LabelType.valueOf(target.getTargetType().name()));
    }
    private TransformTargetLabel(String className, String elementName, String elementDescriptor, LabelType labelType) {
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

    public TransformTargetLabel(String className) {
        this(className, "", "", CLASS);
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

    final LabelType getLabelType() {
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

    public enum LabelType {
        FIELD(FieldNode.class), METHOD(MethodNode.class), CLASS(ClassNode.class);

        private final Class<?> nodeType;

        LabelType(Class<?> nodeType) {
            this.nodeType = nodeType;
        }

        public static Optional<LabelType> getTypeFor(java.lang.reflect.Type type) {
            for (LabelType t : values()) {
                if (t.nodeType.getName().equals(type.getTypeName())) {
                    return Optional.of(t);
                }
            }
            return Optional.empty();
        }

        public Class<?> getNodeType() {
            return nodeType;
        }

        @SuppressWarnings("unchecked")
        public <V> TransformList<V> getFromMap(EnumMap<LabelType, TransformList<?>> transformers) {
            return get(transformers, (Class<V>) this.nodeType);
        }

        @SuppressWarnings("unchecked")
        private <V> TransformList<V> get(EnumMap<LabelType, TransformList<?>> transformers, Class<V> type) {
            return (TransformList<V>) transformers.get(this);
        }

        @SuppressWarnings("unchecked")
        public <T> Supplier<TransformList<T>> mapSupplier(EnumMap<LabelType, TransformList<?>> transformers) {
            return () -> (TransformList<T>) transformers.get(this);
        }

    }
}
