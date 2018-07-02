package com.goeswhere.dmnp.linenos;

import com.goeswhere.dmnp.util.ASMContainers;
import com.goeswhere.dmnp.util.ASMWrapper;
import com.goeswhere.dmnp.util.InsnIter;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.collect.LinkedListMultimap;
import com.google.common.collect.ListMultimap;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Label;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.*;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.IllegalClassFormatException;
import java.lang.instrument.Instrumentation;
import java.lang.instrument.UnmodifiableClassException;
import java.nio.file.Files;
import java.security.ProtectionDomain;
import java.util.Collection;
import java.util.Map.Entry;
import java.util.concurrent.ExecutionException;

/**
 * $ java -Xbootclasspath/p:linenos.jar -javaagent:linenos.jar=package/to/break
 */
public class LineNos implements ClassFileTransformer {
    private final String prefixString;

    public static void main(final String[] args) throws FileNotFoundException, IOException {
        for (final String filename : args) {
            Files.write(new File(filename).toPath(), messWith(ASMWrapper.makeCn(filename)));
        }
    }

    public static void premain(String agentArguments, Instrumentation instrumentation) throws UnmodifiableClassException {
        instrumentation.addTransformer(new LineNos(agentArguments), true);
        try {
            ClassLoader.getSystemClassLoader().loadClass(LineNos.class.getName());
        } catch (ClassNotFoundException e) {
            throw new IllegalStateException("Couldn't find ourselves");
        }
        instrumentation.retransformClasses(StackTraceElement.class);
    }

    public static void printStackTrace(Throwable t) {
        System.err.println(t);
        for (StackTraceElement e : t.getStackTrace()) {
            final int ln = e.getLineNumber();
            if (ln < 1000)
                System.err.println("\tat " + e);
            else {
                System.err.println("\tat " + new StackTraceElement(e.getClassName(), e.getMethodName(), e.getFileName(), ln % 1000) +
                        attemptingTo(e, ln));
            }
        }
    }

    @VisibleForTesting
    static byte[] messWith(ClassNode cn) {
        for (final MethodNode m : ASMContainers.methods(cn)) {
            final ListMultimap<Integer, AbstractInsnNode> create = LinkedListMultimap.create();

            {
                int current = 0;
                for (AbstractInsnNode ins : new InsnIter(m.instructions)) {
                    if (ins instanceof LineNumberNode)
                        current = ((LineNumberNode) ins).line;
                    else if (ins instanceof MethodInsnNode)
                        create.put(current, ins);
                }
            }

            for (Entry<Integer, Collection<AbstractInsnNode>> a : create.asMap().entrySet()) {
                final Collection<AbstractInsnNode> interesting = a.getValue();
                if (interesting.size() <= 1)
                    continue;
                int running = 0;
                for (AbstractInsnNode ins : interesting) {
                    final int line = a.getKey() + ((++running) * 1000);
                    final LabelNode ln = new LabelNode(new Label());

                    m.instructions.insertBefore(ins, ln);
                    m.instructions.insertBefore(ins, new LineNumberNode(line, ln));
                }
            }
        }

        return byteArray(cn);
    }

    private LineNos(String prefixString) {
        this.prefixString = prefixString;
    }

    @Override
    public byte[] transform(ClassLoader loader, String className, Class<?> classBeingRedefined,
                            ProtectionDomain protectionDomain, byte[] classfileBuffer) throws IllegalClassFormatException {
        if ("java/lang/StackTraceElement".equals(className))
            return fixSTE(makeCn(classfileBuffer));
        if (!className.startsWith(prefixString))
            return classfileBuffer;
        final byte[] messed = messWith(makeCn(classfileBuffer));
        contentResolver.put(className.replace('/', '.'), makeCn(messed));
        return messed;
    }

    @VisibleForTesting
    static AbstractInsnNode fromLine(ClassNode cn, int line) {
        boolean now = false;
        for (final MethodNode m : ASMContainers.methods(cn)) {
            for (AbstractInsnNode ins : new InsnIter(m.instructions)) {
                if (ins instanceof LineNumberNode)
                    now = line == ((LineNumberNode) ins).line;
                else if (now)
                    return ins;
            }
        }

        throw new IllegalArgumentException("Couldn't find line " + line);
    }

    private byte[] fixSTE(ClassNode ste) {
        final MethodNode m = ASMContainers.methodMap(ste).get("toString");
        m.instructions.clear();
        m.instructions.add(new VarInsnNode(Opcodes.ALOAD, 0));
        m.instructions.add(new MethodInsnNode(Opcodes.INVOKESTATIC, LineNos.class.getName().replace('.', '/'),
                "stackTraceElementToString", "(Ljava/lang/StackTraceElement;)Ljava/lang/String;"));
        m.instructions.add(new InsnNode(Opcodes.ARETURN));
        return byteArray(ste);
    }

    /**
     * implements {@link #fixSTE(ClassNode)}'s generated call
     */
    public static String stackTraceElementToString(StackTraceElement e) {
        return e.getClassName() + "." + e.getMethodName() +
                (e.isNativeMethod() ? "(Native Method)" :
                        (e.getFileName() != null && e.getLineNumber() >= 0 ?
                                "(" + e.getFileName() + ":" + (e.getLineNumber() > 1000 ? e.getLineNumber() % 1000 : e.getLineNumber()) + ")" :
                                (e.getFileName() != null ? "(" + e.getFileName() + ")" : "(Unknown Source)")))
                + (e.getLineNumber() > 1000 ? attemptingTo(e, e.getLineNumber()) : "");
    }


    @VisibleForTesting
    static final LoadingCache<String, ClassNode> contentResolver =
            CacheBuilder.newBuilder().softValues().build(new CacheLoader<String, ClassNode>() {
                @Override
                public ClassNode load(String name) throws Exception {
                    return ASMWrapper.makeCn(new ClassReader(name));
                }
            });

    @VisibleForTesting
    static String prettify(AbstractInsnNode ins, int ln) {
        if (ins instanceof MethodInsnNode) {
            final MethodInsnNode mi = (MethodInsnNode) ins;
            return "invoke " + mi.name + " #" + (ln / 1000);
        }

        return ins.toString();
    }

    private static String attemptingTo(StackTraceElement e, final int ln) {
        try {
            return ", attempting to " + prettify(LineNos.fromLine(contentResolver.get(e.getClassName()), ln), ln);
        } catch (ExecutionException e1) {
            e1.printStackTrace();
            return ", attempting operation at unknown location; decoding error: " + e1.getCause();
        }
    }

    private static byte[] byteArray(ClassNode cn) {
        final ClassWriter cw = new ClassWriter(0);
        cn.accept(cw);
        return cw.toByteArray();
    }

    private ClassNode makeCn(byte[] classfileBuffer) {
        return ASMWrapper.makeCn(new ClassReader(classfileBuffer));
    }
}
