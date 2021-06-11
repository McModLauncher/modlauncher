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

package cpw.mods.modlauncher.test;

import cpw.mods.modlauncher.*;
import cpw.mods.modlauncher.api.*;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.MarkerManager;
import org.apache.logging.log4j.core.config.Configurator;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.*;
import org.objectweb.asm.*;
import org.objectweb.asm.tree.*;
import org.powermock.reflect.*;

import java.nio.file.*;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test core transformer functionality
 */
class ClassTransformerTests {
    @Test
    void testClassTransformer() throws Exception {
//        Configurator.setRootLevel(Level.TRACE);
//        System.setProperty("modlauncher.logging.marker.classdump", "ACCEPT");
        MarkerManager.getMarker("CLASSDUMP");
        Configurator.setLevel(ClassTransformer.class.getName(), Level.TRACE);
        final TransformStore transformStore = new TransformStore();
        final LaunchPluginHandler lph = new LaunchPluginHandler(null);
        final ClassTransformer classTransformer = Whitebox.invokeConstructor(ClassTransformer.class, new Class[] { transformStore.getClass(),  lph.getClass(), TransformingClassLoader.class }, new Object[] { transformStore, lph, null});
        final ITransformationService dummyService = new MockTransformerService();
        Whitebox.invokeMethod(transformStore, "addTransformer", new TransformTargetLabel("test.MyClass"), classTransformer(), dummyService);
        byte[] result = Whitebox.invokeMethod(classTransformer, "transform", new Class[]{byte[].class, String.class,String.class}, new byte[0], "test.MyClass","testing");
        assertAll("Class loads and is valid",
                () -> assertNotNull(result),
                () -> assertNotNull(new TransformingClassLoader(transformStore, lph, FileSystems.getDefault().getPath(".")).getClass("test.MyClass", result)),
                () ->
                {
                    ClassReader cr = new ClassReader(result);
                    ClassNode cn = new ClassNode();
                    cr.accept(cn, 0);
                    assertTrue(cn.fields.stream().anyMatch(f -> f.name.equals("testfield")));
                }
        );

        ClassNode dummyClass = new ClassNode();
        dummyClass.superName = "java/lang/Object";
        dummyClass.version = 52;
        dummyClass.name = "test/DummyClass";
        dummyClass.fields.add(new FieldNode(Opcodes.ACC_PUBLIC, "dummyfield", "Ljava/lang/String;", null, null));
        ClassWriter cw = new ClassWriter(Opcodes.ASM5);
        dummyClass.accept(cw);
        Whitebox.invokeMethod(transformStore, "addTransformer", new TransformTargetLabel("test.DummyClass", "dummyfield"), fieldNodeTransformer1(), dummyService);
        byte[] result1 = Whitebox.invokeMethod(classTransformer, "transform", new Class[]{byte[].class, String.class, String.class}, cw.toByteArray(), "test.DummyClass", "testing");
        assertAll("Class loads and is valid",
                () -> assertNotNull(result1),
                () -> assertNotNull(new TransformingClassLoader(transformStore, lph, FileSystems.getDefault().getPath(".")).getClass("test.DummyClass", result1)),
                () ->
                {
                    ClassReader cr = new ClassReader(result1);
                    ClassNode cn = new ClassNode();
                    cr.accept(cn, 0);
                    assertEquals("CHEESE", cn.fields.get(0).value);
                }
        );
    }

    private ITransformer<FieldNode> fieldNodeTransformer1() {
        return new ITransformer<FieldNode>() {
            @NotNull
            @Override
            public FieldNode transform(FieldNode input, ITransformerVotingContext context) {
                input.value = "CHEESE";
                return input;
            }

            @NotNull
            @Override
            public TransformerVoteResult castVote(ITransformerVotingContext context) {
                return TransformerVoteResult.YES;
            }

            @NotNull
            @Override
            public Set<Target> targets() {
                return Collections.emptySet();
            }
        };
    }

    private ITransformer<ClassNode> classTransformer() {
        return new ITransformer<ClassNode>() {
            @NotNull
            @Override
            public ClassNode transform(ClassNode input, ITransformerVotingContext context) {
                input.superName="java/lang/Object";
                FieldNode fn = new FieldNode(Opcodes.ACC_PUBLIC, "testfield", "Ljava/lang/String;", null, null);
                input.fields.add(fn);
                return input;
            }

            @NotNull
            @Override
            public TransformerVoteResult castVote(ITransformerVotingContext context) {
                return TransformerVoteResult.YES;
            }

            @NotNull
            @Override
            public Set<Target> targets() {
                return Collections.emptySet();
            }
        };
    }
}
