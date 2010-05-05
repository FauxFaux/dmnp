package com.goeswhere.dmnp.util;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import com.google.common.base.Function;
import com.google.common.base.Joiner;
import com.google.common.base.Predicate;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Multimap;


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
		final List<T> ret = new ArrayList<T>();
		for (T t : it)
			if (pred.apply(t))
				ret.add(t);
		return ret;
	}

	public static <R,T> Iterable<R> map(Iterable<T> col, Function<T,R> f) {
		final List<R> ret = new ArrayList<R>();
		for (T t : col)
			ret.add(f.apply(t));
		return ret;
	}

	public static String intersperse(Iterable<?> what, String with) {
		return Joiner.on(with).join(what);
	}

	public static <R, T> Iterable<R> concatMap(Iterable<T> col, Function<T, Iterable<R>> f) {
		final List<R> ret = new ArrayList<R>();
		for (T t : col)
			for (R r : f.apply(t))
				ret.add(r);
		return ret;
	}

	public static <T> Iterable<T> flattenToSet(final Multimap<T, T> map) {
		return ImmutableSet.<T>builder()
			.addAll(map.keySet())
			.addAll(map.values())
			.build();
	}

	@TerribleImplementation
	public static <T> Iterable<T> cons(T t, Iterable<T> with) {
		return ImmutableList.<T>builder().add(t).addAll(with).build();
	}
}
