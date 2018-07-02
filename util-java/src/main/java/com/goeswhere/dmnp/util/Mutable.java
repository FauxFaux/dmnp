package com.goeswhere.dmnp.util;

import com.google.common.base.Supplier;


public class Mutable<T> implements Supplier<T> {
    private T val;

    public Mutable() {
        this(null);
    }

    private Mutable(T t) {
        set(t);
    }

    public void set(T val) {
        this.val = val;
    }

    @Override
    public T get() {
        return val;
    }

    public static <T> Mutable<T> of(T t) {
        return new Mutable<T>(t);
    }

    public static <T> Mutable<T> create() {
        return of(null);
    }
}
