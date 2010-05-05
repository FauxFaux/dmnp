package com.goeswhere.dmnp.util;

import com.google.common.base.Supplier;


public class Mutable<T> implements Supplier<T> {
	private T val;

	public void set(T val) {
		this.val = val;
	}

	@Override public T get() {
		return val;
	}

}
