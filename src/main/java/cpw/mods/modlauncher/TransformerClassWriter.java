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

import cpw.mods.modlauncher.api.ITransformerActivity;
import cpw.mods.modlauncher.serviceapi.ILaunchPluginService;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.ClassNode;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

class TransformerClassWriter extends ClassWriter {
    private static final Logger LOGGER = LogManager.getLogger();
    private static final Map<String, String> CLASS_PARENTS = new ConcurrentHashMap<>();
    private static final Map<String, Set<String>> CLASS_HIERARCHIES = new ConcurrentHashMap<>();
    private static final Map<String, Boolean> IS_INTERFACE = new ConcurrentHashMap<>();
    private final ClassTransformer classTransformer;
    private final ClassNode clazzAccessor;
    private boolean computedThis = false;

    public static ClassWriter createClassWriter(final int mlFlags, final ClassTransformer classTransformer, final ClassNode clazzAccessor) {
        final int writerFlag = mlFlags & ~ILaunchPluginService.ComputeFlags.SIMPLE_REWRITE; //Strip any modlauncher-custom fields

        //Only use the TransformerClassWriter when needed as it's slower, and only COMPUTE_FRAMES calls getCommonSuperClass
        return (writerFlag & ILaunchPluginService.ComputeFlags.COMPUTE_FRAMES) != 0 ? new TransformerClassWriter(writerFlag, classTransformer, clazzAccessor) : new ClassWriter(writerFlag);
    }

    private TransformerClassWriter(final int writerFlags, final ClassTransformer classTransformer, final ClassNode clazzAccessor) {
        super(writerFlags);
        this.classTransformer = classTransformer;
        this.clazzAccessor = clazzAccessor;
    }

    @Override
    protected String getCommonSuperClass(final String type1, final String type2) {
        if (!computedThis) {
            computeHierarchy(clazzAccessor);
            computedThis = true;
        }

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


    private Set<String> getSupers(final String typeName) {
        computeHierarchy(typeName);
        return CLASS_HIERARCHIES.get(typeName);
    }

    private boolean isIntf(final String typeName) {
        //We don't need computeHierarchy as it has been called already from a different method every time this method is called
        return IS_INTERFACE.get(typeName);
    }

    private String getSuper(final String typeName) {
        computeHierarchy(typeName);
        return CLASS_PARENTS.get(typeName);
    }

    private void computeHierarchy(final ClassNode clazzNode) {
        if (!CLASS_HIERARCHIES.containsKey(clazzNode.name)) {
            clazzNode.accept(new SuperCollectingVisitor());
        }
    }

    /**
     * Computes the hierarchy for a specific class if it has not been computed yet
     */
    private void computeHierarchy(final String className) {
        if (CLASS_HIERARCHIES.containsKey(className)) return; //already computed
        Class<?> clz = classTransformer.getTransformingClassLoader().getLoadedClass(className.replace('/', '.'));
        if (clz != null) {
            computeHierarchyFromClass(className, clz);
        } else {
            computeHierarchyFromFile(className);
        }
    }

    /**
     * Computes the hierarchy for a specific class using the already loaded class object
     * Must be kept in sync with the file counterpart {@link SuperCollectingVisitor#visit(int, int, String, String, String, String[])}
     */
    private void computeHierarchyFromClass(final String name, final Class<?> clazz) {
        Class<?> superClass = clazz.getSuperclass();
        Set<String> hierarchies = new HashSet<>();
        if (superClass != null) {
            String superName = superClass.getName().replace('.', '/');
            CLASS_PARENTS.put(name, superName);
            if (!CLASS_HIERARCHIES.containsKey(superName))
                computeHierarchyFromClass(superName, superClass);
            hierarchies.add(name);
            hierarchies.addAll(CLASS_HIERARCHIES.get(superName));
        } else {
            hierarchies.add("java/lang/Object");
        }
        IS_INTERFACE.put(name, clazz.isInterface());
        Arrays.stream(clazz.getInterfaces()).forEach(c->{
            String n = c.getName().replace('.', '/');
            if (!CLASS_HIERARCHIES.containsKey(n))
                computeHierarchyFromClass(n, c);
            hierarchies.add(n);
            hierarchies.addAll(CLASS_HIERARCHIES.get(n));
        });
        CLASS_HIERARCHIES.put(name, hierarchies); //Only put the set in the map once it is fully populated, to prevent another thread from using incomplete data
    }

    /**
     * Computes the hierarchy for a specific class by loading the class from disk and running it through modlauncher.
     */
    private void computeHierarchyFromFile(final String className) {
        try {
            byte[] classData = classTransformer.getTransformingClassLoader().buildTransformedClassNodeFor(className.replace('/', '.'), ITransformerActivity.COMPUTING_FRAMES_REASON);
            ClassReader classReader = new ClassReader(classData);
            classReader.accept(new SuperCollectingVisitor(), ClassReader.SKIP_CODE | ClassReader.SKIP_DEBUG | ClassReader.SKIP_FRAMES);
        } catch (ClassNotFoundException e) {
            //Don't panic just yet. Do a classload on the super classloader
            //This is safe, as the TCL can't find the class, so it has to be on the super classloader, and it can't cause circulation,
            //as classes from the parent classloader cannot reference classes from the TCL, as the parent only contains libraries and std lib
            try {
                computeHierarchyFromClass(className, Class.forName(className.replace('/', '.'), false, classTransformer.getTransformingClassLoader()));
            } catch (ClassNotFoundException classNotFoundException) {
                classNotFoundException.addSuppressed(e);
                LOGGER.fatal("Failed to find class {} ", className, classNotFoundException);
                throw new RuntimeException("Cannot find class " + className, classNotFoundException);
            }
        }
    }

    private class SuperCollectingVisitor extends ClassVisitor {

        public SuperCollectingVisitor() {
            super(Opcodes.ASM9);
        }

        @Override
        public void visit(final int version, final int access, final String name, final String signature, final String superName, final String[] interfaces) {
            Set<String> hierarchies = new HashSet<>();
            if (superName != null) {
                CLASS_PARENTS.put(name, superName);
                computeHierarchy(superName);
                hierarchies.add(name);
                hierarchies.addAll(CLASS_HIERARCHIES.get(superName));
            } else {
                hierarchies.add("java/lang/Object");
            }
            IS_INTERFACE.put(name, (access & Opcodes.ACC_INTERFACE) != 0);
            Arrays.stream(interfaces).forEach(n->{
                computeHierarchy(n);
                hierarchies.add(n);
                hierarchies.addAll(CLASS_HIERARCHIES.get(n));
            });
            CLASS_HIERARCHIES.put(name, hierarchies); //Only put the set in the map once it is fully populated, to prevent another thread from using incomplete data
        }
    }
}
