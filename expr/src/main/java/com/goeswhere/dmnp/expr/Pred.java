package com.goeswhere.dmnp.expr;

class Pred {

	public static Pred of(Token pop) {
		if (pop instanceof Const) {
			Const c = (Const) pop;
			if (c.val.equals(1))
				return TRUE;
			else if (c.val.equals(0))
				return FALSE;
		} else if (pop instanceof Pred)
			return (Pred)pop;

		throw new AssertionError("Can't pred of that: " + pop);
	}

	final static Pred TRUE = new Pred() {
		@Override public String toString() {
			return "true";
		}
	};

	final static Pred FALSE = new Pred() {
		@Override public String toString() {
			return "false";
		}
	};
}


class PredCmp extends Pred implements Token {
	private final Token left;
	private final Op op;
	private final Token right;

	private PredCmp(Token left, Op op, Token right) {
		this.left = left;
		this.op = op;
		this.right = right;
	}

	static PredCmp of(Token lw, Op ow, Token rw) {
		final Token l, r;
		final Op o;

		if (lw instanceof Const) {
			r = lw;
			l = rw;
			o = ow.switched();
		} else {
			l = lw;
			r = rw;
			o = ow;
		}

		if (l instanceof PredCmp
				&& r instanceof Const) {
			boolean rightIsTrue = ((Const)r).val.equals(1);
			if (Op.EQ == o && rightIsTrue ||
				Op.NE == o && !rightIsTrue)
				return (PredCmp)l;
			if (Op.EQ == o && !rightIsTrue ||
				Op.NE == o && rightIsTrue) {
				final PredCmp comp = (PredCmp)l;
				return comp.inverted();
			}
		}
		return new PredCmp(l, o, r);
	}

	public PredCmp inverted() {
		return new PredCmp(left, op.inverted(), right);
	}

	@Override public String toString() {
		return left + " " + op.rep + " " + right;
	}

	@Override
	public int hashCode() {
		return left.hashCode() * 31 * 31
			+ op.hashCode() * 31
			+ right.hashCode();
	}

	@Override
	public boolean equals(Object obj) {
		PredCmp c = (PredCmp) obj;
		return c.op == op && c.left.equals(left) && c.right.equals(right);
	}

	public boolean strongerThan(PredCmp p) {
		return p.left.equals(left)
			&& right instanceof Const
			&& p.right instanceof Const
			&& op == Op.EQ && p.op == Op.NE;
	}

	@Override
	public int compareTo(Token other) {
		if (!(other instanceof PredCmp))
			return 0;

		final PredCmp p = (PredCmp) other;

		int lct = left.compareTo(p.left);
		if (0 != lct)
			return lct;

		final int oct = op.compareTo(p.op);
		if (0 != oct)
			return oct;

		return right.compareTo(p.right);
	}

}