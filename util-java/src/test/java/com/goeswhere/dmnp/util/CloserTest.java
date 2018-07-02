package com.goeswhere.dmnp.util;

import com.google.common.collect.Lists;
import org.junit.Test;

import java.io.Closeable;
import java.io.PrintStream;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.Assert.assertEquals;

public class CloserTest {
    private final class TestCloseable implements Closeable {
        private final List<Integer> res;
        private final int a;

        private TestCloseable(List<Integer> res, int a) {
            this.res = res;
            this.a = a;
        }

        @Override
        public void close() {
            res.add(a);
            throw new RuntimeException() {
                @Override
                public void printStackTrace(PrintStream s) {
                    // silence, beautiful silence
                }
            };
        }
    }

    @Test
    public void testNothing() {
        new Closer().close();
    }

    @Test
    public void testOne() {
        final List<Integer> res = Lists.newArrayList();
        final Closer c = new Closer();
        try {
            c.add(new TestCloseable(res, 1));
        } finally {
            c.close();
        }
        assertEquals(Collections.singletonList(1), res);
    }

    @Test
    public void testTwo() {
        final List<Integer> res = Lists.newArrayList();
        final Closer c = new Closer();
        try {
            c.add(new TestCloseable(res, 1));
            c.add(new TestCloseable(res, 2));
        } finally {
            c.close();
        }
        assertEquals(Arrays.asList(2, 1), res);
    }
}
