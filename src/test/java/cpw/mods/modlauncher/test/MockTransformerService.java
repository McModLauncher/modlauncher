package cpw.mods.modlauncher.test;

import cpw.mods.modlauncher.api.*;
import joptsimple.*;
import org.objectweb.asm.*;
import org.objectweb.asm.tree.*;

import javax.annotation.*;
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

    @Nonnull
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
    public void beginScanning(final IEnvironment environment) {
        // noop
    }

    @Override
    public void onLoad(IEnvironment env, Set<String> otherServices) throws IncompatibleEnvironmentException {

    }

    @Nonnull
    @Override
    public List<ITransformer> transformers() {
        return Stream.of(new ClassNodeTransformer(modList)).collect(Collectors.toList());
    }

    private static class ClassNodeTransformer implements ITransformer<ClassNode> {
        private final List<String> classNames;

        private ClassNodeTransformer(List<String> classNames) {
            this.classNames = classNames;
        }

        @Nonnull
        @Override
        public ClassNode transform(ClassNode input, ITransformerVotingContext context) {
            FieldNode fn = new FieldNode(Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC, "testfield", "Ljava/lang/String;", null, "CHEESE!");
            input.fields.add(fn);
            return input;
        }

        @Nonnull
        @Override
        public TransformerVoteResult castVote(ITransformerVotingContext context) {
            return TransformerVoteResult.YES;
        }

        @Nonnull
        @Override
        public Set<Target> targets() {
            return classNames.stream().map(Target::targetClass).collect(Collectors.toSet());
        }
    }

}
