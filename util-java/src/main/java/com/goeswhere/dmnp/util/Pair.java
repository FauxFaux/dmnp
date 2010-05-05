package com.goeswhere.dmnp.util;

import com.google.common.base.Objects;

public class Pair<T, U> {

	public final T t;
	public final U u;

	private Pair(T t, U u) {
		this.t = t;
		this.u = u;
	}

	public static<T, U> Pair<T, U> of(T t, U u) {
		return new Pair<T,U>(t,u);
	}

	@Override public String toString() {
		return "Pair [" + t + " and " + u + "]";
	}

	@Override public int hashCode() {
		return t.hashCode() + 31 * u.hashCode();
	}

	@SuppressWarnings("unchecked")
	@Override public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (!(obj instanceof Pair))
			return false;

		final Pair<T, U> pair = (Pair<T, U>)obj;
		return Objects.equal(pair.t, t)
			&& Objects.equal(pair.u, u);
	}
}
