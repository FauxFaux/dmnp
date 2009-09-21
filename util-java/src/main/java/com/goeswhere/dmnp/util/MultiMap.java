package com.goeswhere.dmnp.util;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

public class MultiMap<T,U> {
	private final Map<T, Set<U>> under = new HashMap<T, Set<U>>();

	public Set<U> get(T key) {
		final Set<U> r = under.get(key);
		if (null != r)
			return r;

		final Set<U> newer = new HashSet<U>();
		under.put(key, newer);

		return newer;
	}

	public boolean put(T key, U value) {
		return get(key).add(value);
	}

	public Set<Entry<T, Set<U>>> entrySet() {
		return under.entrySet();
	}

	@Override public String toString() {
		return under.toString();
	}

	public Set<T> keySet() {
		return under.keySet();
	}

	public Collection<Set<U>> values() {
		return under.values();
	}
}
