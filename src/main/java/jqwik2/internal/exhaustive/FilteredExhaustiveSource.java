package jqwik2.internal.exhaustive;

import java.util.*;
import java.util.function.*;

import jqwik2.api.*;
import jqwik2.api.recording.*;

public class FilteredExhaustiveSource<T extends GenSource, E> implements ExhaustiveSource<T> {
	private final ExhaustiveSource<T> exhaustiveSource;
	private final Predicate<E> filter;

	public FilteredExhaustiveSource(ExhaustiveSource<T> exhaustiveSource, Predicate<E> filter) {
		this.exhaustiveSource = exhaustiveSource;
		this.filter = filter;
	}

	@Override
	public long maxCount() {
		return exhaustiveSource.maxCount();
	}

	@Override
	public boolean advance() {
		return exhaustiveSource.advance();
	}

	@Override
	public boolean advanceThisOrUp() {
		return exhaustiveSource.advanceThisOrUp();
	}

	@Override
	public void reset() {
		exhaustiveSource.reset();
	}

	@Override
	public FilteredExhaustiveSource<T, E> clone() {
		return new FilteredExhaustiveSource<>((ExhaustiveSource<T>) exhaustiveSource.clone(), filter);
	}

	@Override
	public void setPrev(Exhaustive<?> exhaustive) {
		exhaustiveSource.setPrev(exhaustive);
	}

	@Override
	public void setSucc(Exhaustive<?> exhaustive) {
		exhaustiveSource.setSucc(exhaustive);
	}

	@Override
	public Optional<Exhaustive<?>> prev() {
		return exhaustiveSource.prev();
	}

	@Override
	public Optional<Exhaustive<?>> succ() {
		return exhaustiveSource.succ();
	}

	@Override
	public Recording recording() {
		return exhaustiveSource.recording();
	}

	@Override
	public Iterator<T> iterator() {
		// TODO: This is not correct, but it's good enough for now
		return exhaustiveSource.iterator();
	}
}
