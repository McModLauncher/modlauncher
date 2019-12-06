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

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.ClassNode;

import java.io.IOException;
import java.io.InputStream;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

class TransformerClassWriter extends ClassWriter {
    private static final Logger LOGGER = LogManager.getLogger();
    private static final HashMap<String,String> classParents = new HashMap<>();
    private static final HashMap<String, Set<String>> classHierarchies = new HashMap<>();
    private static final HashMap<String, Boolean> isInterface = new HashMap<>();
    private ClassTransformer classTransformer;
    private final ClassNode clazzAccessor;

    public TransformerClassWriter(final ClassTransformer classTransformer, final ClassNode clazzAccessor) {
        super(ClassWriter.COMPUTE_FRAMES | Opcodes.ASM7);
        this.classTransformer = classTransformer;
        this.clazzAccessor = clazzAccessor;
        if (!classParents.containsKey(clazzAccessor.name)) {
            computeHierarchy(clazzAccessor);
        }
    }

    private Set<String> getSupers(final String typeName) {
        if (!classParents.containsKey(typeName)) {
            computeHierarchy(typeName, classTransformer);
        }
        return classHierarchies.get(typeName);
    }

    private boolean isIntf(final String typeName) {
        if (!classParents.containsKey(typeName)) {
            computeHierarchy(typeName, classTransformer);
        }
        return isInterface.get(typeName);
    }

    private String getSuper(final String typeName) {
        if (!classParents.containsKey(typeName)) {
            computeHierarchy(typeName, classTransformer);
        }
        return classParents.get(typeName);
    }

    private void computeHierarchy(final ClassNode clazzNode) {
        clazzNode.accept(new SuperCollectingVisitor(classTransformer));
    }

    private void computeHierarchy(final String className, final ClassTransformer classTransformer) {
        final String target = className.replace('.', '/').concat(".class");
        InputStream resource = null;
        try {
            resource = classTransformer.getTransformingClassLoader().getResourceAsStream(target);
            final ClassReader classReader = new ClassReader(resource);
            classReader.accept(new SuperCollectingVisitor(classTransformer), ClassReader.SKIP_CODE | ClassReader.SKIP_DEBUG | ClassReader.SKIP_FRAMES);
        } catch (IOException e) {
            LOGGER.fatal("Class {} unable to find resource {}", className, resource);
            throw new RuntimeException("Failed to load hierarchy member " + className, e);
        } catch (NullPointerException e) {
            // discard NPE - it's because the classloader doesn't exist in testing
            classParents.put(className, "java/lang/Object");
            classHierarchies.put(className, Stream.of(className, "java/lang/Object").collect(Collectors.toSet()));
        }
        finally {
            try {resource.close();} catch (NullPointerException | IOException e) {}
        }
    }

    @Override
    protected String getCommonSuperClass(String type1, String type2) {
        if (getSupers(type2).contains(type1)) {
            return type1;
        }
        if (getSupers(type1).contains(type2)) {
            return type2;
        }

        if (isIntf(type1) || isIntf(type2)) {
            return "java/lang/Object";
        }

        String type = type1;
        do {
            type = getSuper(type);
        } while (!getSupers(type2).contains(type));
        return type;
    }

    private class SuperCollectingVisitor extends ClassVisitor {
        private final ClassTransformer classTransformer;

        public SuperCollectingVisitor(final ClassTransformer classTransformer) {
            super(Opcodes.ASM7);
            this.classTransformer = classTransformer;
        }

        @Override
        public void visit(final int version, final int access, final String name, final String signature, final String superName, final String[] interfaces) {
            classParents.put(name, superName);
            if (superName != null) {
                computeHierarchy(superName, classTransformer);
                classHierarchies.put(name, Stream.concat(Stream.of(name), classHierarchies.get(superName).stream()).collect(Collectors.toSet()));
            } else {
                classHierarchies.put(name, Collections.singleton("java/lang/Object"));
            }
            isInterface.put(name, (access & Opcodes.ACC_INTERFACE) != 0);
            Arrays.stream(interfaces).forEach(n->{
                computeHierarchy(n, classTransformer);
                classHierarchies.get(name).add(n);
                classHierarchies.get(name).addAll(classHierarchies.get(n));
            });
        }
    }
}
