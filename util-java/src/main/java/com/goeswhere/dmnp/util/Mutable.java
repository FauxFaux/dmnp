package com.goeswhere.dmnp.util;


public class Mutable<T> {
	private T val;

	public void set(T val) {
		this.val = val;
	}

	public T get() {
		return val;
	}

}
