package com.goeswhere.dmnp.expr;

import java.util.Iterator;

class SList<T> implements
		Iterable<T>,
		Comparable<Iterable<T>> {
	private final T value;
	private final SList<T> tail;

	private SList(SList<T> tail, T t) {
		this.tail = tail;
		this.value = t;
	}

	static <V> SList<V> head(V v) {
		return new SList<V>(null, v);
	}

	SList<T> plus(T u) {
		return new SList<T>(this, u);
	}

	SList<T> plusAll(Iterable<T> l) {
		SList<T> ret = this;
		for (T q : l)
			ret = ret.plus(q);
		return ret;
	}

	@Override
	public Iterator<T> iterator() {
		return new Iterator<T>() {
			SList<T> list = SList.this;
			@Override public boolean hasNext() {
				return null != list;
			}

			@Override public T next() {
				final T q = list.value;
				list = list.tail;
				return q;
			}

			@Override public void remove() {
				throw new UnsupportedOperationException();
			}
		};
	}

	@Override
	public String toString() {
		final StringBuilder sb = new StringBuilder("[");
		Iterator<T> it = iterator();
		if (it.hasNext())
			sb.append(it.next());
		while (it.hasNext())
			sb.append(", ").append(it.next());
		sb.append("]");
		return sb.toString();
	}

	public static <V> SList<V> empty() {
		return new SList<V>(null, null) {
			@Override SList<V> plus(V t) {
				return head(t);
			}
		};
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((tail == null) ? 0 : tail.hashCode());
		result = prime * result + ((value == null) ? 0 : value.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;

		final SList<?> other = (SList<?>)obj;
		if (tail == null) {
			if (other.tail != null)
				return false;
		} else if (!tail.equals(other.tail))
			return false;

		if (value == null) {
			if (other.value != null)
				return false;
		} else if (!value.equals(other.value))
			return false;
		return true;
	}

	@Override
	public int compareTo(Iterable<T> other) {
		Iterator<T> us = iterator(), them = other.iterator();
		while (us.hasNext() && them.hasNext()) {
			final T un = us.next(), tn = them.next();
			if (un instanceof Comparable<?>) {
				final int comp =
					((Comparable)un).compareTo(tn);
				if (0 != comp)
					return comp;
			}
		}

		if (them.hasNext())
			return 1;
		if (us.hasNext())
			return -1;

		return 0;
	}
}