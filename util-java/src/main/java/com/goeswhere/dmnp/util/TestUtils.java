package com.goeswhere.dmnp.util;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;

public class TestUtils {

	/** Locking and unlocking do nothing at all. */
	public static final Lock EMPTY_LOCK = new Lock() {

		@Override public void lock() {
			// nothing at all
		}

		@Override public void lockInterruptibly() throws InterruptedException {
			// nothing at all
		}

		@Override public boolean tryLock() {
			return true;
		}

		@Override public boolean tryLock(long time, TimeUnit unit) throws InterruptedException {
			return true;
		}

		@Override public void unlock() {
			// nothing at all
		}

		@Override public Condition newCondition() {
			throw new UnsupportedOperationException();
		}
	};


	public static String cleanWhitespace(String string) {
		return string.replaceAll("[\\s]+", " ");
	}
}
