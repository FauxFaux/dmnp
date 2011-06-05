package com.goeswhere.dmnp.util;

import static com.goeswhere.dmnp.util.Containers.hasBit;

import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.MethodNode;

import com.google.common.base.Function;
import com.google.common.collect.Lists;

public class ASMContainers {

	private ASMContainers() {
		throw new AssertionError();
	}

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

	@SuppressWarnings("unchecked")
	public static List<FieldNode> fields(ClassNode cn) {
		return cn.fields;
	}

	@SuppressWarnings("unchecked")
	public static List<String> interfaces(ClassNode cn) {
		return cn.interfaces;
	}

	public static final Function<FieldNode, String> NAME_FIELDNODE = new Function<FieldNode, String>() {
		@Override public String apply(FieldNode from) {
			return from.name;
		}
	};
	public static final Function<MethodNode, String> NAME_METHODNODE = new Function<MethodNode, String>() {
		@Override public String apply(MethodNode from) {
			return from.name;
		}
	};

	public static List<String> fieldNames(final ClassNode cn) {
		return Lists.transform(fields(cn), NAME_FIELDNODE);
	}

	public static Map<String, MethodNode> methodMap(final ClassNode cn) {
		return Containers.toMap(methods(cn), NAME_METHODNODE);
	}


	public static boolean isPrivate(final int acc) {
		return hasBit(acc, Opcodes.ACC_PRIVATE);
	}

	public static boolean isDefault(final int acc) {
		return !isProtected(acc) && !isPrivate(acc) && !isPublic(acc);
	}

	public static boolean isProtected(final int acc) {
		return hasBit(acc, Opcodes.ACC_PROTECTED);
	}

	public static boolean isPublic(final int acc) {
		return hasBit(acc, Opcodes.ACC_PUBLIC);
	}

	public static String packageOf(String name) {
		return name.replaceFirst("/?([^/]*?)$", "");
	}

	public static boolean isStatic(final int acc) {
		return hasBit(acc, Opcodes.ACC_STATIC);
	}

	public static boolean isFinal(final int acc) {
		return hasBit(acc, Opcodes.ACC_FINAL);
	}

	public static boolean isAbstract(final int acc) {
		return hasBit(acc, Opcodes.ACC_ABSTRACT);
	}

	public static String packageOf(ClassNode cn) {
		return packageOf(cn.name);
	}
}
