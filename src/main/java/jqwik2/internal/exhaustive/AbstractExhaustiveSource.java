package jqwik2.internal.exhaustive;

import jqwik2.api.*;

abstract class AbstractExhaustiveSource<T extends GenSource> extends AbstractExhaustive<ExhaustiveSource<T>> implements ExhaustiveSource<T> {

	private boolean hasNext = true;

	@Override
	public boolean hasNext() {
		return hasNext;
	}

	@Override
	public T next() {
		T next = get();
		hasNext = advance();
		return next;
	}
}
