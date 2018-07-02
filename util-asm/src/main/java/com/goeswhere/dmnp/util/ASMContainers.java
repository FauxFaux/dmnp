package com.goeswhere.dmnp.util;

import com.google.common.base.Function;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.*;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static com.goeswhere.dmnp.util.Containers.hasBit;

public class ASMContainers {

    private ASMContainers() {
        throw new AssertionError();
    }

    @SuppressWarnings("unchecked")
    public static Iterable<AbstractInsnNode> it(final InsnList insns) {
        return () -> insns.iterator();
    }

    @SuppressWarnings("unchecked")
    public static List<MethodNode> methods(ClassNode cn) {
        return cn.methods;
    }

    @SuppressWarnings("unchecked")
    private static List<FieldNode> fields(ClassNode cn) {
        return cn.fields;
    }

    @SuppressWarnings("unchecked")
    public static List<String> interfaces(ClassNode cn) {
        return cn.interfaces;
    }

    private static final Function<FieldNode, String> NAME_FIELDNODE = from -> from.name;
    private static final Function<MethodNode, String> NAME_METHODNODE = from -> from.name;

    public static List<String> fieldNames(final ClassNode cn) {
        return fields(cn).stream().map(NAME_FIELDNODE).collect(Collectors.toList());
    }

    public static Map<String, MethodNode> methodMap(final ClassNode cn) {
        return Containers.toMap(methods(cn), NAME_METHODNODE);
    }


    private static boolean isPrivate(final int acc) {
        return hasBit(acc, Opcodes.ACC_PRIVATE);
    }

    public static boolean isDefault(final int acc) {
        return !isProtected(acc) && !isPrivate(acc) && !isPublic(acc);
    }

    private static boolean isProtected(final int acc) {
        return hasBit(acc, Opcodes.ACC_PROTECTED);
    }

    private static boolean isPublic(final int acc) {
        return hasBit(acc, Opcodes.ACC_PUBLIC);
    }

    private static String packageOf(String name) {
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
