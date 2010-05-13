package com.goeswhere.dmnp.util;

import java.util.Map;

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
}
