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

package cpw.mods.modlauncher.benchmarks;

import cpw.mods.modlauncher.*;
import cpw.mods.modlauncher.api.ITransformerActivity;
import cpw.mods.modlauncher.serviceapi.ILaunchPluginService;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.ClassNode;
import org.openjdk.jmh.annotations.*;
import org.powermock.reflect.Whitebox;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Method;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;

import static cpw.mods.modlauncher.api.LambdaExceptionUtils.uncheck;

@State(Scope.Benchmark)
public class TransformBenchmark {
    public volatile ClassTransformer classTransformer;
    static Method transform;
    byte[] classBytes;

    @Setup
    public void setup() throws Exception {
        final TransformStore transformStore = new TransformStore();
        final ModuleLayerHandler layerHandler = Whitebox.invokeConstructor(ModuleLayerHandler.class);
        final LaunchPluginHandler lph = new LaunchPluginHandler(layerHandler);
        classTransformer = uncheck(()->Whitebox.invokeConstructor(ClassTransformer.class, new Class[] { transformStore.getClass(),  lph.getClass(), TransformingClassLoader.class }, new Object[] { transformStore, lph, null}));
        transform = uncheck(()->classTransformer.getClass().getDeclaredMethod("transform", byte[].class, String.class,String.class));
        transform.setAccessible(true);
        Map<String, ILaunchPluginService> plugins = Whitebox.getInternalState(lph, "plugins");
        try (InputStream is = getClass().getClassLoader().getResourceAsStream("cpw/mods/modlauncher/testjar/TestClass.class")) {
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            byte[] buf = new byte[2048];
            while (is.read(buf) >= 0) {
                bos.write(buf);
            }
            classBytes = bos.toByteArray();
        } catch (IOException e) {
            e.printStackTrace();
        }
        plugins.put("dummy1", new ILaunchPluginService() {
            @Override
            public String name() {
                return "dummy1";
            }

            @Override
            public boolean processClass(final Phase phase, final ClassNode classNode, final Type classType) {
                return true;
            }

            @Override
            public <T> T getExtension() {
                return null;
            }

            @Override
            public EnumSet<Phase> handlesClass(final Type classType, final boolean isEmpty) {
                return EnumSet.of(Phase.BEFORE, Phase.AFTER);
            }
        });
    }

    @Benchmark
    public int transformNoop() {
        byte[] result = uncheck(()->(byte[])transform.invoke(classTransformer,new byte[0], "test.MyClass","jmh"));
        return result.length + 1;
    }

    @TearDown(Level.Iteration)
    public void clearLog() {
        TransformerAuditTrail auditTrail = Whitebox.getInternalState(classTransformer, "auditTrail");
        Map<String, List<ITransformerActivity>> map = Whitebox.getInternalState(auditTrail, "audit");
        map.clear();
    }

    @Benchmark
    public int transformDummyClass() {
        byte[] result = uncheck(()->(byte[])transform.invoke(classTransformer,classBytes, "cpw.mods.modlauncher.testjar.TestClass","jmh"));
        return result.length + 1;
    }
}
