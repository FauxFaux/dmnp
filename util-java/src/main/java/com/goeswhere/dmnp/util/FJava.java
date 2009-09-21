package com.goeswhere.dmnp.util;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;


public class FJava {
	private FJava() {
		// nothing at all
	}

	public static interface Func1<R,T> {
		abstract R apply(T t);
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
			if (pred.matches(t))
				ret.add(t);
		return ret;
	}

	public static <R,T> Iterable<R> map(Iterable<T> col, Func1<R,T> f) {
		final List<R> ret = new ArrayList<R>();
		for (T t : col)
			ret.add(f.apply(t));
		return ret;
	}

	public static String intersperse(Iterable<String> filter,
			String string) {
		final StringBuilder sb = new StringBuilder();
		final Iterator<String> it = filter.iterator();
		if (it.hasNext())
			sb.append(it.next());
		while (it.hasNext())
			sb.append(string).append(it.next());
		return sb.toString();
	}

	public static <R, T> Iterable<R> concatMap(Iterable<T> col, Func1<Iterable<R>, T> f) {
		final List<R> ret = new ArrayList<R>();
		for (T t : col)
			for (R r : f.apply(t))
				ret.add(r);
		return ret;
	}

	public static <T> Iterable<T> flatten(final MultiMap<T, T> map) {
		return flatten(cons(map.keySet(), map.values()));
	}

	@TerribleImplementation
	public static <T> Iterable<T> cons(T t, Iterable<T> with) {
		final List<T> ret = new ArrayList<T>();
		ret.add(t);
		for (T q : with)
			ret.add(q);
		return ret;
	}

	@TerribleImplementation
	private static <T> Iterable<T> flatten(Iterable<? extends Iterable<T>> it) {
		final List<T> ret = new ArrayList<T>();
		for (Iterable<T> t : it)
			for (T q : t)
				ret.add(q);
		return ret;
	}

	/** sep = "|" gives: [] = "", [a] = "a", [a,b,c] = "a|b|c". */
	public static String join(Iterable<?> lst, String sep) {
		final Iterator<?> it = lst.iterator();
		if (!it.hasNext())
			return "";

		final StringBuilder sb = new StringBuilder();
		sb.append(it.next());
		while (it.hasNext())
			sb.append(sep).append(it.next());
		return sb.toString();
	}

}
