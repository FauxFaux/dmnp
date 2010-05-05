package com.goeswhere.dmnp.util;


public class Containers {
	public static boolean hasBit(int what, int bit) {
		return (what & bit) == bit;
	}
}
