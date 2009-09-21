package com.goeswhere.dmnp.util;

import static com.goeswhere.dmnp.util.Containers.set;
import static com.goeswhere.dmnp.util.FJava.cons;
import static com.goeswhere.dmnp.util.FJava.flatten;
import static java.util.Arrays.asList;
import static org.junit.Assert.assertEquals;

import java.util.Arrays;

import org.junit.Test;

public class FJavaTest {
	private final static MultiMap<Integer, Integer> TEST_MULTIMAP =
		new MultiMap<Integer, Integer>() {{
			put(5, 6);
			put(5, 6);
			put(5, 7);
			put(7, 8);
			put(7, 9);
	}};

	@Test public void multiMapKeys() {
		assertEquals(set(5,7), TEST_MULTIMAP.keySet());
	}

	@Test public void multiMapValues() {
		assertEquals(set(set(6,7), set(8,9)), set(TEST_MULTIMAP.values()));
	}

	@Test public void multiMapFlatten() {
		assertEquals(asList(5, 7, 6, 7, 8, 9),
			flatten(TEST_MULTIMAP));
	}

	@Test public void testCons() {
		assertEquals(asList("5"), cons("5", asList()));
		assertEquals(asList("5", "7"), cons("5", asList("7")));
		assertEquals(Arrays.asList("5", "6", "7"), cons("5", asList("6", "7")));
		assertEquals(Arrays.asList("5", "7", "6"), cons("5", cons("7", cons("6", asList()))));
	}
}
