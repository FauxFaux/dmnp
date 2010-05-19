package com.goeswhere.dmnp.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotSame;

import java.util.Arrays;
import java.util.Iterator;

import org.junit.Test;

import com.google.common.collect.Iterators;
import com.google.common.collect.Lists;

public class BetweenIterableTest {
	@Test public void docs() {
		final Iterable<Integer> bi = BetweenIterable.of(Arrays.asList(1,2,3,4,5,6,7), 2, 6);
		assertIterable(bi);
		assertEquals(Arrays.asList(3,4,5), Lists.newArrayList(bi));
	}

	private static <T> void assertIterable(Iterable<T> bi) {
		Iterator<T> left = bi.iterator(), right = bi.iterator();
		assertNotSame(left, right);
		Iterators.elementsEqual(left, right);
	}
}
