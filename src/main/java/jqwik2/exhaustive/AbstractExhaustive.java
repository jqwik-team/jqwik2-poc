package jqwik2.exhaustive;

import java.util.*;

import jqwik2.api.*;

abstract class AbstractExhaustive<T extends Exhaustive<T>> implements Exhaustive<T> {

	private Exhaustive<?> succ = null;
	private Exhaustive<?> prev = null;

	@Override
	public void next() {
		if (succ().isEmpty()) {
			advance();
		} else {
			succ.next();
		}
	}

	@Override
	public void advance() {
		if (tryAdvance()) {
			return;
		}
		reset();
		if (prev().isPresent()) {
			prev().get().advance();
		} else {
			Generator.noMoreValues();
		}
	}

	protected abstract boolean tryAdvance();

	@Override
	public void setSucc(Exhaustive<?> exhaustive) {
		this.succ = exhaustive;
	}

	@Override
	public void setPrev(Exhaustive<?> exhaustive) {
		this.prev = exhaustive;
	}

	@Override
	public Optional<Exhaustive<?>> prev() {
		return Optional.ofNullable(prev);
	}

	@Override
	public Optional<Exhaustive<?>> succ() {
		return Optional.ofNullable(succ);
	}

	@Override
	public T clone() {
		throw new UnsupportedOperationException("clone() must be overridden in subclasses");
	}
}
