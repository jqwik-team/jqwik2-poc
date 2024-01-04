package jqwik2.internal.exhaustive;

import java.util.*;

import jqwik2.api.*;

abstract class AbstractExhaustive<T extends Exhaustive<T>> implements Exhaustive<T> {

	private Exhaustive<?> succ = null;
	private Exhaustive<?> prev = null;

	@Override
	public boolean advanceChain() {
		if (succ().isEmpty()) {
			return advance();
		} else {
			return succ.advanceChain();
		}
	}

	@Override
	public boolean advance() {
		if (tryAdvance()) {
			return true;
		}
		reset();
		if (prev().isPresent()) {
			return prev().get().advance();
		} else {
			return false;
		}
	}

	/**
	 * Try to advance this exhaustive source locally.
	 * Return true if successful, false if exhausted.
	 */
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
