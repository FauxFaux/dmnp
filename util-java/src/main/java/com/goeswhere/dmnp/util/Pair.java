package com.goeswhere.dmnp.util;



public class Pair<T, U> {

	public final T t;
	public final U u;

	public Pair(T t, U u) {
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
		return t.hashCode() ^ u.hashCode();
	}

	@Override public boolean equals(Object obj) {
		Pair<T, U> pair = (Pair<T, U>)obj;
		return (pair.t == t && pair.u == u)
			|| (pair.t.equals(t) && pair.u.equals(u));
	}
}
