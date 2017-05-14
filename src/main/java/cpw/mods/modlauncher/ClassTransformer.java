package cpw.mods.modlauncher;

import cpw.mods.modlauncher.api.Transformer;
import cpw.mods.modlauncher.api.VoteResult;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldNode;
import org.objectweb.asm.tree.MethodNode;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Transforms classes using the supplied launcher services
 */
public class ClassTransformer
{
    private final TransformStore transformers;
    private static final byte[] EMPTY = new byte[0];

    ClassTransformer(TransformStore transformers)
    {
        this.transformers = transformers;
    }

    byte[] transform(byte[] inputClass, String className)
    {
        Type classDesc = Type.getObjectType(className.replaceAll("\\.", "/"));
        if (!transformers.needsTransforming(className))
        {
            return inputClass;
        }

        ClassNode clazz = new ClassNode(Opcodes.ASM5);
        byte[] digest;
        boolean empty;
        if (inputClass.length > 0)
        {
            final ClassReader classReader = new ClassReader(inputClass);
            classReader.accept(clazz, 0);
            digest = getSha256().digest(inputClass);
            empty = false;
        }
        else
        {
            clazz.name = classDesc.getInternalName();
            clazz.version = 52;
            clazz.superName = "java/lang/Object";
            digest = getSha256().digest(EMPTY);
            empty = true;
        }
        VotingContext context = new VotingContext(className, empty, digest);

        List<FieldNode> fieldList = new ArrayList<>(clazz.fields.size());
        // it's probably possible to inject "dummy" fields into this list for spawning new fields without class transform
        for (FieldNode field : clazz.fields)
        {
            List<Transformer<FieldNode>> fieldTransformers = new ArrayList<>(transformers.getTransformersFor(className, field));
            fieldList.add(this.performVote(fieldTransformers, field, context));
        }

        // it's probably possible to inject "dummy" methods into this list for spawning new methods without class transform
        List<MethodNode> methodList = new ArrayList<>(clazz.methods.size());
        for (MethodNode method : clazz.methods)
        {
            List<Transformer<MethodNode>> methodTransformers = new ArrayList<>(transformers.getTransformersFor(className, method));
            methodList.add(this.performVote(methodTransformers, method, context));
        }

        clazz.fields = fieldList;
        clazz.methods = methodList;
        List<Transformer<ClassNode>> classTransformers = new ArrayList<>(transformers.getTransformersFor(className));
        clazz = this.performVote(classTransformers, clazz, context);

        ClassWriter cw = new ClassWriter(Opcodes.ASM5);
        clazz.accept(cw);

        return cw.toByteArray();
    }

    private <T> T performVote(List<Transformer<T>> transformers, T node, VotingContext context)
    {
        do
        {
            final Stream<Vote<T>> voteResultStream = transformers.stream().map(t -> gatherVote(t, context));
            final Map<VoteResult, List<Vote<T>>> results = voteResultStream.collect(Collectors.groupingBy(Vote::getResult));
            if (results.containsKey(VoteResult.REJECT))
            {
                throw new VoteRejectedException(results.get(VoteResult.REJECT), node.getClass());
            }
            if (results.containsKey(VoteResult.NO))
            {
                transformers.removeAll(results.get(VoteResult.NO).stream().map(Vote::getTransformer).collect(Collectors.toList()));
            }
            if (results.containsKey(VoteResult.YES))
            {
                final Transformer<T> transformer = results.get(VoteResult.YES).get(0).getTransformer();
                node = transformer.transform(node, context);
                transformers.remove(transformer);
                continue;
            }
            if (results.containsKey(VoteResult.DEFER))
            {
                throw new VoteDeadlockException(results.get(VoteResult.DEFER), node.getClass());
            }
        }
        while (!transformers.isEmpty());
        return node;
    }

    private <T> Vote<T> gatherVote(Transformer<T> transformer, VotingContext context)
    {
        VoteResult vr = transformer.castVote(context);
        return new Vote<>(vr, transformer);
    }

    private MessageDigest getSha256()
    {
        try
        {
            return MessageDigest.getInstance("SHA-256");
        }
        catch (NoSuchAlgorithmException e)
        {
            throw new RuntimeException("HUH");
        }
    }
}
