package com.goeswhere.dmnp.util;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import com.google.common.base.Function;
import com.google.common.collect.Maps;


public class Containers {
	public static boolean hasBit(int what, int bit) {
		return (what & bit) == bit;
	}

	/** Copy iterable into a map; m[f(v)] = v; */
	public static <K,V> Map<K,V> toMap(Iterable<? extends V> what, Function<V,K> func) {
		final Map<K,V> m = Maps.newHashMap();
		for (V v : what)
			m.put(func.apply(v), v);
		return m;
	}

	public static String classAndToString(Object p) {
		return p == null ? String.valueOf(null) : p.getClass() + ": " + p;
	}

	public static String hrList(final List<?> el, final String and) {
		final StringBuilder sb = new StringBuilder(10 * el.size());
		for (int i = 0; i < el.size(); ++i) {
			sb.append(el.get(i));
			if (i == el.size() - 2)
				sb.append(and);
			else if (i != el.size() - 1)
				sb.append(", ");
		}
		return sb.toString();
	}

	public static String hrList(final List<?> el) {
		return hrList(el, " and ");
	}

	public static <T> Set<T> newConcurrentHashSet() {
		return Collections.newSetFromMap(new ConcurrentHashMap<T, Boolean>());
	}

	public static <K,V> Map<K, V> newConcurrentHashMap() {
		return new ConcurrentHashMap<K, V>();
	}

}
