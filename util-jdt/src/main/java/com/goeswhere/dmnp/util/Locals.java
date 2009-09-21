package com.goeswhere.dmnp.util;

import java.util.HashMap;
import java.util.Map;

import org.eclipse.jdt.core.dom.Type;

public class Locals<T> {

	Map<String, T> con = new HashMap<String, T>();
	private final Locals<T> locals;

	public Locals(Locals<T> locals) {
		this.locals = locals;
	}

	public Locals() {
		locals = null;
	}

	public void add(Type type, String identifier, T v) {
		if (contains(identifier))
			throw new DuplicateLocalException("already have a '" + identifier + "' local");
		con.put(identifier, v);
	}

	@Override public String toString() {
		return "Locals [con=" + con + ", locals=" + locals + "]";
	}

	public boolean contains(String ident) {
		Locals<T> l = this;

		while (null != l) {
			if (l.con.containsKey(ident))
				return true;
			l = l.locals;
		}
		return false;
	}

	public T get(String ident) {
		Locals<T> l = this;

		while (null != l) {
			if (l.con.containsKey(ident))
				return l.con.get(ident);
			l = l.locals;
		}

		throw new AssertionError("Asked for contents of local variable " + ident + " which didn't exist");
	}

	public void set(String ident, T value) {
		Locals l = this;

		while (null != l) {
			if (l.con.containsKey(ident)) {
				l.con.put(ident, value);
				break;
			}
			l = l.locals;
		}
		if (null == l)
			throw new AssertionError("Asked to set local variable '" + ident + "' which didn't exist");
	}
}

class DuplicateLocalException extends RuntimeException {
	public DuplicateLocalException(String msg) {
		super(msg);
	}
}