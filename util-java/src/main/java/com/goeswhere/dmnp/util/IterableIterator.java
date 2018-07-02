package com.goeswhere.dmnp.util;

import java.util.Iterator;

class IterableIterator<T> implements Iterable<T> {

    private final Iterator<T> it;

    private IterableIterator(Iterator<T> it) {
        this.it = it;
    }

    public static <T> Iterable<T> once(Iterator<T> it) {
        return new IterableIterator<>(it);
    }

    @Override
    public Iterator<T> iterator() {
        return it;
    }

}
