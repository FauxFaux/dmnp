package com.goeswhere.dmnp.util;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

public class Containers {

	public static boolean equal(Object l, Object r) {
		if (null == l && null == r)
			return true;
		if (null == l ^ null == r)
			return false;
		return r != null && r.equals(l);
	}

	public static boolean hasBit(int what, int bit) {
		return (what & bit) == bit;
	}

	public static <T>Set<T> set() {
		return Collections.emptySet();
	}

	public static <T>Set<T> set(Iterable<T> one) {
		final Set<T> ret = new HashSet<T>();
		for (T t : one)
			ret.add(t);
		return ret;
	}

	public static Set<String> set(String... string) {
		final Set<String> res = new HashSet<String>();
		for (String s : string)
			res.add(s);
		return res;
	}

	public static <T>Set<T> set(T one) {
		final Set<T> ret = new HashSet<T>();
		ret.add(one);
		return ret;
	}

	public static <T>Set<T> set(T one, T two) {
		final Set<T> ret = new HashSet<T>();
		ret.add(one);
		ret.add(two);
		return ret;
	}

}
