package com.goeswhere.dmnp.util;

import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.InsnList;

import java.util.Iterator;

abstract class CopyableIterator<T> implements Iterator<T>, Iterable<T> {
    @Override
    public abstract CopyableIterator<T> iterator();

    @Override
    public void remove() {
        throw new UnsupportedOperationException();
    }
}

public class InsnIter extends CopyableIterator<AbstractInsnNode> {

    private final InsnList instructions;
    private int pos;

    public InsnIter(InsnList instructions) {
        this(instructions, 0);
    }

    private InsnIter(InsnList instructions, int pos) {
        this.instructions = instructions;
        this.pos = pos;
    }

    @Override
    public boolean hasNext() {
        return pos < instructions.size();
    }

    @Override
    public AbstractInsnNode next() {
        return instructions.get(pos++);
    }

    @Override
    public InsnIter iterator() {
        return new InsnIter(instructions, pos);
    }

}