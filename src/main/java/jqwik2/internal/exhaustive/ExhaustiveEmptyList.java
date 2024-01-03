package jqwik2.internal.exhaustive;

import jqwik2.api.*;

public class ExhaustiveEmptyList extends ExhaustiveList {

	public ExhaustiveEmptyList() {
		// TODO: This is a hack to make ExhaustiveList work with empty lists
		super(0, null);
	}

	public int size() {
		return 0;
	}

	@Override
	public long maxCount() {
		return 1;
	}

	@Override
	public void advance() {
		if (prev().isEmpty()) {
			Generator.noMoreValues();
		}
		prev().get().advance();
	}

	@Override
	public ExhaustiveSource<GenSource.List> clone() {
		return new ExhaustiveEmptyList();
	}

	@Override
	public void next() {
		advance();
	}

	@Override
	public void setSucc(Exhaustive<?> exhaustive) {
		// ignore
	}

	@Override
	public String toString() {
		return "ExhaustiveEmptyList";
	}
}