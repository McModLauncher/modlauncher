package cpw.mods.modlauncher;

import cpw.mods.modlauncher.api.*;
import org.objectweb.asm.*;
import org.objectweb.asm.tree.*;

import java.security.*;
import java.util.*;
import java.util.stream.*;

/**
 * Transforms classes using the supplied launcher services
 */
public class ClassTransformer {
    private static final byte[] EMPTY = new byte[0];
    private final TransformStore transformers;

    ClassTransformer(TransformStore transformers) {
        this.transformers = transformers;
    }

    byte[] transform(byte[] inputClass, String className) {
        Type classDesc = Type.getObjectType(className.replaceAll("\\.", "/"));
        if (!transformers.needsTransforming(className)) {
            return inputClass;
        }

        ClassNode clazz = new ClassNode(Opcodes.ASM5);
        byte[] digest;
        boolean empty;
        if (inputClass.length > 0) {
            final ClassReader classReader = new ClassReader(inputClass);
            classReader.accept(clazz, 0);
            digest = getSha256().digest(inputClass);
            empty = false;
        } else {
            clazz.name = classDesc.getInternalName();
            clazz.version = 52;
            clazz.superName = "java/lang/Object";
            digest = getSha256().digest(EMPTY);
            empty = true;
        }
        VotingContext context = new VotingContext(className, empty, digest);

        List<FieldNode> fieldList = new ArrayList<>(clazz.fields.size());
        // it's probably possible to inject "dummy" fields into this list for spawning new fields without class transform
        for (FieldNode field : clazz.fields) {
            List<ITransformer<FieldNode>> fieldTransformers = new ArrayList<>(transformers.getTransformersFor(className, field));
            fieldList.add(this.performVote(fieldTransformers, field, context));
        }

        // it's probably possible to inject "dummy" methods into this list for spawning new methods without class transform
        List<MethodNode> methodList = new ArrayList<>(clazz.methods.size());
        for (MethodNode method : clazz.methods) {
            List<ITransformer<MethodNode>> methodTransformers = new ArrayList<>(transformers.getTransformersFor(className, method));
            methodList.add(this.performVote(methodTransformers, method, context));
        }

        clazz.fields = fieldList;
        clazz.methods = methodList;
        List<ITransformer<ClassNode>> classTransformers = new ArrayList<>(transformers.getTransformersFor(className));
        clazz = this.performVote(classTransformers, clazz, context);

        ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_FRAMES | Opcodes.ASM5);
        clazz.accept(cw);

        return cw.toByteArray();
    }

    private <T> T performVote(List<ITransformer<T>> transformers, T node, VotingContext context) {
        do {
            final Stream<TransformerVote<T>> voteResultStream = transformers.stream().map(t -> gatherVote(t, context));
            final Map<TransformerVoteResult, List<TransformerVote<T>>> results = voteResultStream.collect(Collectors.groupingBy(TransformerVote::getResult));
            if (results.containsKey(TransformerVoteResult.REJECT)) {
                throw new VoteRejectedException(results.get(TransformerVoteResult.REJECT), node.getClass());
            }
            if (results.containsKey(TransformerVoteResult.NO)) {
                transformers.removeAll(results.get(TransformerVoteResult.NO).stream().map(TransformerVote::getTransformer).collect(Collectors.toList()));
            }
            if (results.containsKey(TransformerVoteResult.YES)) {
                final ITransformer<T> transformer = results.get(TransformerVoteResult.YES).get(0).getTransformer();
                node = transformer.transform(node, context);
                transformers.remove(transformer);
                continue;
            }
            if (results.containsKey(TransformerVoteResult.DEFER)) {
                throw new VoteDeadlockException(results.get(TransformerVoteResult.DEFER), node.getClass());
            }
        }
        while (!transformers.isEmpty());
        return node;
    }

    private <T> TransformerVote<T> gatherVote(ITransformer<T> transformer, VotingContext context) {
        TransformerVoteResult vr = transformer.castVote(context);
        return new TransformerVote<>(vr, transformer);
    }

    private MessageDigest getSha256() {
        try {
            return MessageDigest.getInstance("SHA-256");
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("HUH");
        }
    }
}
