package com.goeswhere.dmnp.expr;

import org.objectweb.asm.Opcodes;

public enum Op {
	EQ("="), NE("<>"),
	GT(">"), GTE(">="),
	LT("<"), LTE("<=");

	final String rep;

	Op(String rep) {
		this.rep = rep;
	}
	@Override public String toString() {
		return super.toString() + " (" + rep + ")";
	}

	Op inverted() {
		switch (this) {
		case EQ:
			return NE;
		case GT:
			return LTE;
		case GTE:
			return LT;
		case LT:
			return GTE;
		case LTE:
			return GT;
		case NE:
			return EQ;
		}
		throw new AssertionError();
	}

	public static Op back(int opcode) {
		switch (opcode) {
		case Opcodes.IF_ICMPNE:
		case Opcodes.IF_ACMPNE:
		case Opcodes.IFNE:
			return Op.EQ;
		case Opcodes.IF_ICMPEQ:
		case Opcodes.IF_ACMPEQ:
		case Opcodes.IFEQ:
			return Op.NE;
		case Opcodes.IF_ICMPGE:
		case Opcodes.IFGE:
			return Op.LT;
		case Opcodes.IF_ICMPGT:
			return Op.LTE;
		case Opcodes.IF_ICMPLE:
		case Opcodes.IFLE:
			return Op.GT;
		case Opcodes.IF_ICMPLT:
			return Op.GTE;
		}
		throw new AssertionError();
	}

	public Op switched() {
		switch (this) {
			case EQ:
			case NE:
				return this;
			case LT:
				return GT;
			case LTE:
				return GTE;
			case GT:
				return LT;
			case GTE:
				return LTE;
		}
		throw new AssertionError();
	}
}

