package com.goeswhere.dmnp.expr;

interface Token extends Comparable<Token> {
	// tag
}

class Const implements Token {
	final Object val;

	public Const(Object val) {
		this.val = val;
	}

	@Override public String toString() {
		final String stringed = String.valueOf(val);
		if (val instanceof Number)
			return stringed;
		return "'" + stringed + "'";
	}

	@Override
	public int hashCode() {
		return ((val == null) ? 0 : val.hashCode());
	}

	@Override
	public boolean equals(Object obj) {
		if (!(obj instanceof Const))
			return false;
		Const other = (Const) obj;
		if (val == null) {
			if (other.val != null)
				return false;
		} else if (!val.equals(other.val))
			return false;
		return true;
	}

	@Override
	public int compareTo(Token o) {
		if (o instanceof Const) {
			Const c = (Const) o;
			if (equals(o))
				return 0;
			// so terrible, upcast to number etc.
			return String.valueOf(val).compareTo(String.valueOf(c.val));
		}

		return 0;
	}
}

class Var implements Token {

	private final String name;

	public Var(String name) {
		this.name = name;
	}

	@Override public String toString() {
		return name;
	}

	@Override public int hashCode() {
		return name.hashCode();
	}

	@Override public boolean equals(Object obj) {
		return name.equals(((Var)obj).name);
	}

	@Override
	public int compareTo(Token o) {
		if (o instanceof Var)
			return name.compareTo(((Var)o).name);
		return 0;
	}
}

class LocalVar implements Token {

	final int num;

	public LocalVar(int num) {
		this.num = num;
	}

	@Override
	public int hashCode() {
		return -num;
	}

	@Override
	public boolean equals(Object obj) {
		return num == ((LocalVar) obj).num;
	}

	@Override
	public int compareTo(Token o) {
		// Should never be hit, silly hierarchy
		return 0;
	}
}
