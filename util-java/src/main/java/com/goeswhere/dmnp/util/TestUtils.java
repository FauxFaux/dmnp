package com.goeswhere.dmnp.util;

import com.google.common.base.Objects;

import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;

public class TestUtils {

    /**
     * Locking and unlocking do nothing at all.
     */
    public static final Lock EMPTY_LOCK = new Lock() {

        @Override
        public void lock() {
            // nothing at all
        }

        @Override
        public void lockInterruptibly() {
            // nothing at all
        }

        @Override
        public boolean tryLock() {
            return true;
        }

        @Override
        public boolean tryLock(long time, TimeUnit unit) {
            return true;
        }

        @Override
        public void unlock() {
            // nothing at all
        }

        @Override
        public Condition newCondition() {
            throw new UnsupportedOperationException();
        }
    };


    public static String cleanWhitespace(String string) {
        return string.replaceAll("[\\s]+", " ");
    }

    /**
     * assertEquals() with better error messages and a better signature.
     */
    public static <T, U> void assertMapEquals(Map<T, U> expected, Map<T, U> actual) {
        for (Entry<T, U> a : expected.entrySet())
            if (!actual.containsKey(a.getKey()))
                throw new AssertionError("expected: " + a + " not found");
            else {
                final U act = actual.get(a.getKey());
                if (!Objects.equal(act, a.getValue()))
                    throw new AssertionError("for " + a.getKey()
                            + ", expected: " + a.getValue() + ", got: " + act);
            }

        for (Entry<T, U> a : actual.entrySet())
            if (!expected.containsKey(a.getKey()))
                throw new AssertionError("unexpected in actual: " + a);
    }

    public static <K, V> void assertContainsAll(Map<K, V> what, Map<K, V> m) {
        for (Entry<K, V> a : what.entrySet()) {
            final V got = m.get(a.getKey());
            if (null == got)
                throw new AssertionError("didn't contain " + a.getKey());
            if (!got.equals(a.getValue()))
                throw new AssertionError(a.getKey() + " mapped to " + got + ", not " + a.getValue());
        }
    }
}
