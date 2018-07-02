package com.goeswhere.dmnp.util;

import java.util.Iterator;

public class IterableIterator<T> implements Iterable<T> {

    private final Iterator<T> it;

    private IterableIterator(Iterator<T> it) {
        this.it = it;
    }

    public static <T> Iterable<T> once(Iterator<T> it) {
        return new IterableIterator<T>(it);
    }

    @Override
    public Iterator<T> iterator() {
        return it;
    }

}
