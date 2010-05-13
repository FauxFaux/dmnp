package com.goeswhere.dmnp.util;

import java.util.Iterator;

import com.google.common.collect.AbstractIterator;

public class BetweenIterable<T> implements Iterable<T> {
	private final Iterable<? extends T> lis;
	private final T start;
	private final T end;

	/** of([1,2,3,4,5,6,7], 2, 6) == [3,4,5] */
	public static <U> BetweenIterable<U> of(Iterable<? extends U> it, U start, U end) {
		return new BetweenIterable<U>(it, start, end);
	}

	private BetweenIterable(Iterable<? extends T> lis, T start, T end) {
		this.lis = lis;
		this.start = start;
		this.end = end;
	}

	@Override public Iterator<T> iterator() {
		final Iterator<? extends T> it = lis.iterator();
		while (it.hasNext())
			if (it.next().equals(start))
				break;

		return new AbstractIterator<T>() {
			@Override protected T computeNext() {
				if (!it.hasNext())
				    return endOfData();
			    final T next = it.next();
				if (next.equals(end))
					return endOfData();
				return next;
			}
		};
	}
}