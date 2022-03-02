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

import cpw.mods.jarhandling.SecureJar;
import cpw.mods.modlauncher.api.*;
import joptsimple.*;
import org.jetbrains.annotations.NotNull;
import org.objectweb.asm.*;
import org.objectweb.asm.tree.*;

import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.util.*;
import java.util.function.*;
import java.util.stream.*;

/**
 * Test Launcher Service
 */
public class MockTransformerService implements ITransformationService {
    private ArgumentAcceptingOptionSpec<String> modsList;
    private ArgumentAcceptingOptionSpec<Integer> modlists;
    private List<String> modList;
    private String state;

    @NotNull
    @Override
    public String name() {
        return "test";
    }

    @Override
    public void arguments(BiFunction<String, String, OptionSpecBuilder> argumentBuilder) {
        modsList = argumentBuilder.apply("mods", "CSV list of mods to load").withRequiredArg().withValuesSeparatedBy(",").ofType(String.class);
    }

    @Override
    public void argumentValues(OptionResult result) {
        modList = result.values(modsList);
    }

    @Override
    public void initialize(IEnvironment environment) {
        state = "INITIALIZED";
    }

    @Override
    public void onLoad(IEnvironment env, Set<String> otherServices) throws IncompatibleEnvironmentException {

    }

    @Override
    public List<Resource> beginScanning(IEnvironment environment) {
        if (System.getProperty("testJars.location")!=null) {
            SecureJar testjar = SecureJar.from(Path.of(System.getProperty("testJars.location")));
            return List.of(new Resource(IModuleLayerManager.Layer.PLUGIN, List.of(testjar)));
        } else if (System.getProperty("test.harness")!=null) {
            return List.of(new Resource(IModuleLayerManager.Layer.PLUGIN,
                    Arrays.stream(System.getProperty("test.harness").split(","))
                    .map(FileSystems.getDefault()::getPath)
                    .map(SecureJar::from)
                    .toList()));
        } else {
            return List.of();
        }
    }

    @Override
    public List<Resource> completeScan(IModuleLayerManager layerManager) {
        if (System.getProperty("testJars.location")!=null) {
            SecureJar testjar = SecureJar.from(Path.of(System.getProperty("testJars.location")));
            return List.of(new Resource(IModuleLayerManager.Layer.GAME, List.of(testjar)));
        } else {
            return List.of();
        }
    }

    @NotNull
    @Override
    public List<ITransformer> transformers() {
        return Stream.of(new ClassNodeTransformer(modList)).collect(Collectors.toList());
    }

    static class ClassNodeTransformer implements ITransformer<ClassNode> {
        private final List<String> classNames;

        ClassNodeTransformer(List<String> classNames) {
            this.classNames = classNames;
        }

        @NotNull
        @Override
        public ClassNode transform(ClassNode input, ITransformerVotingContext context) {
            FieldNode fn = new FieldNode(Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC, "testfield", "Ljava/lang/String;", null, "CHEESE!");
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
        public Set<Target<ClassNode>> targets() {
            return classNames.stream().map(Target::targetClass).collect(Collectors.toSet());
        }

        @Override
        public TargetType<ClassNode> getTargetType() {
            return TargetType.CLASS;
        }
    }

}
