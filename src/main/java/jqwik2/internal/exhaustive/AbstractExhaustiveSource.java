package jqwik2.internal.exhaustive;

import java.util.*;

import jqwik2.api.*;

public abstract class AbstractExhaustiveSource<T extends GenSource> extends AbstractExhaustive<ExhaustiveSource<T>> implements ExhaustiveSource<T> {

	@Override
	public Iterator<T> iterator() {
		return new Iterator<T>() {
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
}
