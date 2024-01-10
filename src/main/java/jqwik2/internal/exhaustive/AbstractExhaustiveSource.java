package jqwik2.internal.exhaustive;

import java.util.*;
import java.util.concurrent.atomic.*;
import java.util.stream.*;

import jqwik2.api.*;

public abstract class AbstractExhaustiveSource<T extends GenSource> extends AbstractExhaustive<ExhaustiveSource<T>> implements ExhaustiveSource<T> {

	@Override
	public Iterator<T> iterator() {
		return new Iterator<>() {
			private boolean hasNext = true;
			private ExhaustiveSource<T> exhaustive = AbstractExhaustiveSource.this.clone();

			@Override
			public boolean hasNext() {
				return hasNext;
			}

			@Override
			public T next() {
				T next = exhaustive.current();
				hasNext = exhaustive.advance();
				return next;
			}
		};
	}

	protected long sumUpToLongMAX(LongStream longStream) {
		AtomicLong sum = new AtomicLong(0);
		longStream.takeWhile(l -> {
			if (l <= (Long.MAX_VALUE - sum.get())) {
				sum.addAndGet(l);
			} else {
				sum.set(Long.MAX_VALUE);
			}
			return sum.get() < Long.MAX_VALUE;
		}).count();
		return sum.get();
	}

}
