package com.goeswhere.dmnp.util;

import com.google.common.base.Function;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import org.junit.Test;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodNode;

import java.io.IOException;
import java.util.Arrays;
import java.util.Map;
import java.util.Map.Entry;

import static org.junit.Assert.assertEquals;

public class ASMWrapperTest {
    @Test
    public void compile() throws IOException, FailedException {
        final Map<String, ClassNode> nodes = ASMWrapper.compileToClassNodes("class A { void foo() { } }");
        final Entry<String, ClassNode> nodeentry = Iterables.getOnlyElement(nodes.entrySet());
        final ClassNode node = nodeentry.getValue();
        assertEquals("A", nodeentry.getKey());
        assertEquals("A", node.name);
        assertEquals(Arrays.asList("<init>", "foo"),
                Lists.transform(ASMContainers.methods(node),
                        from -> from.name));
    }

    @Test
    public void methodFromClass() throws IOException {
        assertEquals("hashCode", ASMWrapper.getMethod(new Object(), "hashCode").name);
    }
}
