package com.goeswhere.dmnp.util;

import com.google.common.collect.Iterators;
import com.google.common.collect.Lists;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Iterator;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;

class BetweenIterableTest {
    @Test
    void docs() {
        final Iterable<Integer> bi = BetweenIterable.of(Arrays.asList(1, 2, 3, 4, 5, 6, 7), 2, 6);
        assertIterable(bi);
        assertEquals(Arrays.asList(3, 4, 5), Lists.newArrayList(bi));
    }

    private static <T> void assertIterable(Iterable<T> bi) {
        Iterator<T> left = bi.iterator(), right = bi.iterator();
        assertNotSame(left, right);
        Iterators.elementsEqual(left, right);
    }
}
