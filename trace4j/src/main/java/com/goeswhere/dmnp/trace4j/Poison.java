package com.goeswhere.dmnp.trace4j;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.IllegalClassFormatException;
import java.lang.instrument.Instrumentation;
import java.security.ProtectionDomain;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.FieldVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.commons.EmptyVisitor;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldInsnNode;
import org.objectweb.asm.tree.FieldNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.LdcInsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;

import com.goeswhere.dmnp.util.ASMContainers;
import com.goeswhere.dmnp.util.ASMWrapper;

public class Poison implements ClassFileTransformer {

	private static final String LEVEL = "trace";

	static final String LOG4J_CLASSSLASHES = "org/apache/log4j/Logger";
	static final String LOG4J_DESC = "L" + LOG4J_CLASSSLASHES + ";";
	private static final int PRIVATE_FINAL_STATIC = Opcodes.ACC_PRIVATE + Opcodes.ACC_FINAL + Opcodes.ACC_STATIC;

    private final String prefixString;

    public static void main(final String[] args) throws FileNotFoundException, IOException {
		final String filename = args[0];
		writeOut(filename, addLogging(makeCn(filename)));
	}

	private static void writeOut(final String filename, final byte[] byteArray) throws FileNotFoundException,
			IOException {
		final OutputStream fos = new FileOutputStream(filename);

		try {
			fos.write(byteArray);
			fos.flush();
		} finally {
			fos.close();
		}
	}

	private static ClassNode makeCn(final String filename) throws FileNotFoundException, IOException {
		final InputStream s = new FileInputStream(filename);

		final ClassNode cn;
		try {
			cn = makeCn(s);
		} finally {
			s.close();
		}
		return cn;
	}

    private static byte[] addLogging(final ClassNode cn) {

        final String loggerName = ensureLoggerName(cn);

		for (MethodNode m : ASMContainers.methods(cn)) {
			if (m.name.startsWith("<")) // init etc.
				continue;

			if (0 == m.instructions.size())
				continue;

			m.instructions.insert(generateLoggerCall(loggerName, cn.name, m.name + ": entering"));

			final Iterator<AbstractInsnNode> j = ASMContainers.it(m.instructions).iterator();

			while (j.hasNext()) {
				final AbstractInsnNode in = j.next();
				final int op = in.getOpcode();
				if ((op >= Opcodes.IRETURN && op <= Opcodes.RETURN) || op == Opcodes.ATHROW)
					m.instructions.insert(in.getPrevious(),
							generateLoggerCall(loggerName, cn.name, m.name + ": leaving"));
			}
			fiddleTheStack(m);
		}

		final ClassWriter cw = new ClassWriter(0);
		cn.accept(cw);
		final byte[] byteArray = cw.toByteArray();
        return byteArray;
    }

    private static ClassNode makeCn(InputStream file) throws IOException, FileNotFoundException {
        return ASMWrapper.makeCn(new ClassReader(file));
    }

	private static InsnList generateLoggerCall(String loggerName, String classInternalName, String message) {
		InsnList il = new InsnList();
		il.add(new FieldInsnNode(Opcodes.GETSTATIC, classInternalName, loggerName, LOG4J_DESC));
		il.add(new LdcInsnNode(message));
		il.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL, LOG4J_CLASSSLASHES, LEVEL, "(Ljava/lang/Object;)V"));
		return il;
	}

	private static String ensureLoggerName(final ClassNode cn) {
		final List<String> loggers = new ArrayList<String>(1);
		final Set<String> fields = new HashSet<String>(cn.fields.size());

		cn.accept(new EmptyVisitor() {
			@Override public FieldVisitor visitField(int access, String name, String desc, String signature,
					Object value) {
				if (null == signature && LOG4J_DESC.equals(desc)) {
					if (PRIVATE_FINAL_STATIC != access)
						warn(cn.name + "'s " + name + " logger should be private static final");
					loggers.add(name);
				}
				fields.add(name);
				return null;
			}
		});

		return ensureLoggerName(cn, loggers, fields);
	}

	private static String ensureLoggerName(final ClassNode cn, final List<String> loggers, final Set<String> fields) {
		final String loggerName;
		final int size = loggers.size();
		if (0 == size) {
			loggerName = "logger";
			if (fields.contains(loggerName))
				throw new RuntimeException("Was going to create a logger field, but it already exists?");
			addLoggerField(cn, loggerName);
		} else if (1 == size)
			loggerName = loggers.get(0);
		else
			throw new RuntimeException("Too many loggers: " + loggers);
		return loggerName;
	}

	private static void addLoggerField(final ClassNode cn, final String loggerName) {
		addField(cn, new FieldNode(PRIVATE_FINAL_STATIC, loggerName, LOG4J_DESC, null, null));

		for (MethodNode m : ASMContainers.methods(cn)) {
			if (m.name.equals("<clinit>")) {
				initialiseLoggerAsFirstStatement(cn, loggerName, m);
				return;
			}
		}

		final MethodNode m = new MethodNode(Opcodes.ACC_STATIC, "<clinit>", "()V", null, new String[0]);
		initialiseLoggerAsFirstStatement(cn, loggerName, m);
		m.instructions.add(new InsnNode(Opcodes.RETURN));
		fiddleTheStack(m);
		ASMContainers.methods(cn).add(m);

	}

	private static void fiddleTheStack(final MethodNode m) {
		m.maxStack += 4;
	}

	private static void initialiseLoggerAsFirstStatement(final ClassNode cn, final String loggerName, MethodNode m) {
		final InsnList il = new InsnList();
		il.add(new LdcInsnNode(Type.getType("L" + cn.name + ";")));
		il.add(new MethodInsnNode(Opcodes.INVOKESTATIC, LOG4J_CLASSSLASHES, "getLogger", "(Ljava/lang/Class;)Lorg/apache/log4j/Logger;"));
		il.add(new FieldInsnNode(Opcodes.PUTSTATIC, cn.name, loggerName, LOG4J_DESC));
		if (m.instructions.size() == 0)
			m.instructions.add(il);
		else
			m.instructions.insertBefore(m.instructions.get(0), il);
	}

	private static void warn(String s) {
		System.out.println(s);
	}

	@SuppressWarnings("unchecked")
	static void addField(ClassNode cn, FieldNode fieldNode) {
		cn.fields.add(fieldNode);
	}

	static class MutableInt {
		int t;
	}


	public Poison(String prefixString) {
        this.prefixString = prefixString;
    }

    public static void premain(String agentArguments, Instrumentation instrumentation) {
        instrumentation.addTransformer(new Poison(agentArguments));
    }

    @Override
    public byte[] transform(ClassLoader loader, String className, Class<?> classBeingRedefined,
            ProtectionDomain protectionDomain, byte[] classfileBuffer) throws IllegalClassFormatException {
        if (!className.startsWith(prefixString))
            return classfileBuffer;
        return addLogging(ASMWrapper.makeCn(new ClassReader(classfileBuffer)));
    }

}
