package com.goeswhere.dmnp.util;

import static com.goeswhere.dmnp.util.FJava.cons;
import static com.goeswhere.dmnp.util.FJava.intersperse;
import static java.util.Arrays.asList;
import static org.junit.Assert.assertEquals;

import java.util.Arrays;

import org.junit.Test;

public class FJavaTest {

	@Test public void testCons() {
		assertEquals(asList("5"), cons("5", asList()));
		assertEquals(asList("5", "7"), cons("5", asList("7")));
		assertEquals(Arrays.asList("5", "6", "7"), cons("5", asList("6", "7")));
		assertEquals(Arrays.asList("5", "7", "6"), cons("5", cons("7", cons("6", asList()))));
	}

	@Test public void testIntersperse() {
		assertEquals("a", intersperse(Arrays.asList("a"), ","));
		assertEquals("a,b,c", intersperse(Arrays.asList("a", "b", "c"), ","));
	}
}
