package com.goeswhere.dmnp.util;

public class Assert {

	public static void equals(int l, int r) {
		isTrue(l == r);
	}

	private static void isTrue(boolean b) {
		if (!b)
			throw new AssertionError();
	}

	public static void notTrue(boolean b) {
		if (b)
			throw new AssertionError();
	}

}