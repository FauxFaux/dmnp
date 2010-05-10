package com.goeswhere.dmnp.trace4j;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import org.junit.Test;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.commons.EmptyVisitor;
import org.objectweb.asm.tree.ClassNode;

import com.goeswhere.dmnp.util.ASMContainers;
import com.goeswhere.dmnp.util.ASMWrapper;
import com.goeswhere.dmnp.util.TerribleImplementation;
import com.google.common.collect.Lists;

public class PoisonTest {

	@Test public void simple() throws IOException, InstantiationException, IllegalAccessException {
		final byte[] a = Poison.addLogging(ASMWrapper.refToCn(new A()));
		attempInstantiation(A.class, a);

		final ClassNode cn = ASMWrapper.makeCn(new ClassReader(a));
		assertTrue(ASMContainers.fieldNames(cn).contains("logger"));

		final String trace = "true:org/apache/log4j/Logger#trace(Ljava/lang/Object;)V";
		final String print = "true:java/io/PrintStream#println(Ljava/lang/String;)V";
		assertEquals(Arrays.asList(trace, print, trace), callsWhat(cn, "foo"));

		final String aName = A.class.getName().replaceAll("\\.", "/");
		assertEquals(Arrays.asList(trace, "true:" + aName + "#foo()V", trace), callsWhat(cn, "bar"));
	}

	/** @return weirdly formatted string indicating what methods the named method calls, in order. */
	@TerribleImplementation
	private List<String> callsWhat(final ClassNode cn, final String name) {
		final List<String> res = Lists.newArrayList();
		ASMContainers.methodMap(cn).get(name).instructions.accept(new EmptyVisitor() {
			@Override public void visitMethodInsn(int opcode, String owner, String methodname, String desc) {
				res.add((Opcodes.INVOKEVIRTUAL == opcode) + ":" + owner + "#" + methodname + desc);
			}
		});
		return res;
	}

	private void attempInstantiation(final Class<A> type, final byte[] bytes) throws InstantiationException,
			IllegalAccessException {
		new ClassLoader(getClass().getClassLoader()) {
			Class<?> make() {
				return defineClass(type.getName(), bytes, 0, bytes.length);
			}
		}.make().newInstance();
	}


	public static class A {
		void foo() {
			System.out.println("hi");
		}

		void bar() {
			foo();
		}
	}
}
