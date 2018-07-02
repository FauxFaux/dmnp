package com.goeswhere.dmnp.expr;

import com.goeswhere.dmnp.util.*;
import com.google.common.base.Predicate;
import org.objectweb.asm.Label;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.*;

import java.io.IOException;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Set;
import java.util.TreeSet;

import static com.goeswhere.dmnp.util.BytecodeNames.unsupported;
import static com.goeswhere.dmnp.util.FJava.*;


public class Expression {
    private static final Const CONST_ZERO = new Const(0);

    static String toSQL(Predicate<?> x) throws IOException {
        return format(toExpression(x));
    }

    static Iterable<SList<Pred>> toExpression(Predicate<?> x) throws IOException {
        final ClassNode cn = ASMWrapper.refToCn(x);
        final Iterable<MethodNode> candidates =
                filter(ASMContainers.methods(cn), method -> {
                    // this is really weak, but thinking is hard
                    // synthetic will drop the un-generisized method.
                    return !Containers.hasBit(method.access, Opcodes.ACC_SYNTHETIC) &&
                            "apply".equals(method.name) &&
                            method.desc.endsWith(")Z");
                });

        return toExpression(only(candidates).instructions);
    }

    private static Iterable<SList<Pred>> toExpression(InsnList instructions) {
        return recurse(new InsnIter(instructions),
                new ArrayDeque<>(),
                SList.empty());
    }

    static SList<SList<Pred>> recurse(InsnIter code, Deque<Token> localStack, SList<Pred> soFar) {
        while (true) {
            final AbstractInsnNode ins = code.next();
            final int opcode = ins.getOpcode();

            if (-1 == opcode) { // line number, frame, etc.

            } else if (ins instanceof LdcInsnNode) {
                localStack.push(new Const(((LdcInsnNode) ins).cst));
            } else if (Opcodes.ALOAD == opcode) {
                localStack.push(new LocalVar(((VarInsnNode) ins).var));
            } else if (Opcodes.INVOKEVIRTUAL == opcode) {
                final MethodInsnNode m = (MethodInsnNode) ins;
                if (m.name.equals("equals")
                        && m.owner.equals("java/lang/String")
                        && m.desc.equals("(Ljava/lang/Object;)Z")) {
                    localStack.push(PredCmp.of(localStack.pop(), Op.EQ, localStack.pop()));
                } else
                    throw new AssertionError("Can't call normal methods here");
            } else if (opcode == Opcodes.GETFIELD) {
                final FieldInsnNode f = (FieldInsnNode) ins;
                final Token pop = localStack.pop();
                Assert.equals(1, ((LocalVar) pop).num);
                localStack.push(new Var(f.name));
            } else if (Opcodes.BIPUSH == opcode
                    || Opcodes.SIPUSH == opcode) {
                localStack.push(new Const(
                        ((IntInsnNode) ins).operand));
            } else if (ins instanceof JumpInsnNode) {
                final Label label = ((JumpInsnNode) ins).label.getLabel();

                if (opcode == Opcodes.GOTO)
                    return recurse(findLabel(code, label), localStack, soFar);

                if (opcode >= Opcodes.IFEQ && opcode <= Opcodes.IFLE)
                    localStack.push(CONST_ZERO);

                final Token right = localStack.pop();
                final Token left = localStack.pop();
                final Op op = Op.back(opcode);
                final PredCmp pred = PredCmp.of(left, op, right);
                return recurse(findLabel(code.iterator(), label), copy(localStack), soFar.plus(pred.inverted()))
                        .plusAll(recurse(code, localStack, soFar.plus(pred)));

            } else if (ins instanceof InsnNode) {
                if (opcode >= Opcodes.ICONST_M1 && opcode <= Opcodes.ICONST_5) {
                    localStack.push(new Const(opcode - Opcodes.ICONST_M1 - 1));
                } else if (Opcodes.IRETURN == opcode) {
                    return SList.head(soFar.plus(Pred.of(localStack.pop())));
                } else
                    throw unsupported(opcode);
            } else
                throw unsupported(opcode);
        }
    }

    private static InsnIter findLabel(InsnIter it, Label label) {
        while (it.hasNext()) {
            final AbstractInsnNode next = it.next();
            if (next instanceof LabelNode &&
                    ((LabelNode) next).getLabel()
                            .equals(label))
                return it;
        }
        throw new AssertionError("Unsupported non-forward jump");
    }

    private static String format(Iterable<SList<Pred>> r) {
        return "(" + intersperse(map(map(sortedSet(filter(r,
                l -> {
                    for (Pred p : l)
                        if (Pred.FALSE == p)
                            return false;
                    return true;
                })), t -> filter(t, p -> {
                    for (Pred q : t)
                        if (p instanceof PredCmp &&
                                q instanceof PredCmp &&
                                ((PredCmp) q).strongerThan((PredCmp) p))
                            return false;
                    return true;
                })),
                t -> intersperse(map(sortedSet(filter(t, q -> Pred.TRUE != q)), u -> u.toString()), " AND ")), ") OR\n(") + ")";
    }

    private static <T> Set<T> sortedSet(Iterable<T> iter) {
        final Set<T> ret = new TreeSet<>();
        for (T t : iter)
            ret.add(t);
        return ret;
    }

    private static ArrayDeque<Token> copy(Deque<Token> stack) {
        return new ArrayDeque<>(stack);
    }
}
