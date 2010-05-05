package com.goeswhere.dmnp.util;

import static org.junit.Assert.assertEquals;

import java.io.IOException;
import java.util.Arrays;
import java.util.Map;
import java.util.Map.Entry;

import org.junit.Test;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodNode;

import com.google.common.base.Function;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;

public class ASMWrapperTest {
	@Test public void compile() throws IOException, FailedException {
		final Map<String, ClassNode> nodes = ASMWrapper.compileToClassNodes("class A { void foo() { } }");
		final Entry<String, ClassNode> nodeentry = Iterables.getOnlyElement(nodes.entrySet());
		final ClassNode node = nodeentry.getValue();
		assertEquals("A", nodeentry.getKey());
		assertEquals("A", node.name);
		assertEquals(Arrays.asList("<init>", "foo"),
				Lists.transform(ASMContainers.methods(node),
					new Function<MethodNode, String>() {
						@Override public String apply(MethodNode from) {
							return from.name;
						}
		}));
	}

	@Test public void methodFromClass() throws IOException {
		assertEquals("hashCode", ASMWrapper.getMethod(new Object(), "hashCode").name);
	}
}
