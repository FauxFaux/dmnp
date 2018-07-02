package com.goeswhere.dmnp.util;

import com.google.common.base.Function;
import com.google.common.base.Joiner;
import com.google.common.base.Predicate;
import com.google.common.collect.*;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;


public class FJava {
    private FJava() {
        // nothing at all
    }

    public static <T> T only(Iterable<T> col) {
        final Iterator<T> it = col.iterator();
        final T t = it.next();
        Assert.notTrue(it.hasNext());
        return t;
    }

    public static <T> Iterable<T> filter(Iterable<T> it, Predicate<T> pred) {
        final List<T> ret = new ArrayList<>();
        for (T t : it)
            if (pred.apply(t))
                ret.add(t);
        return ret;
    }

    public static <R, T> Iterable<R> map(Iterable<T> col, Function<T, R> f) {
        final List<R> ret = new ArrayList<>();
        for (T t : col)
            ret.add(f.apply(t));
        return ret;
    }

    public static String intersperse(Iterable<?> what, String with) {
        return Joiner.on(with).join(what);
    }

    public static <R, T> Iterable<R> concatMap(Iterable<T> col, Function<T, Iterable<R>> f) {
        return Iterables.concat(Iterables.transform(col, f));
    }

    public static <T> Iterable<T> flattenToSet(final Multimap<T, T> map) {
        return ImmutableSet.<T>builder()
                .addAll(map.keySet())
                .addAll(map.values())
                .build();
    }

    public static <T> Iterable<T> cons(T t, Iterable<? extends T> with) {
        return Iterables.concat(ImmutableList.of(t), with);
    }

    /**
     * Repeatedly apply the function to it's own outputs until it starts
     * returning nothing and gather all the intermediates.
     * <pre>
     * f(x)=y,z; f(y)=q; f(z)= ; f(q)=r
     * reducer(x,f) -> [x,y,z,q,r]
     * </pre>
     */
    @TerribleImplementation
    public static <R> Iterable<R> reducer(R in, Function<R, Iterable<R>> func) {
        final List<R> ret = Lists.newArrayList();
        ret.add(in);
        for (R r : func.apply(in))
            Iterables.addAll(ret, reducer(r, func));
        return ret;
    }

}
