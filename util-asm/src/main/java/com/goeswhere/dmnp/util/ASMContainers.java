package com.goeswhere.dmnp.util;

import java.util.Iterator;
import java.util.List;

import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.MethodNode;

public class ASMContainers {

	@SuppressWarnings("unchecked")
	public static Iterable<AbstractInsnNode> it(final InsnList insns) {
		return new Iterable<AbstractInsnNode>() {
			@Override public Iterator<AbstractInsnNode> iterator() {
				return insns.iterator();
			}
		};
	}

	@SuppressWarnings("unchecked")
	public static List<MethodNode> methods(ClassNode cn) {
		return cn.methods;
	}
}
