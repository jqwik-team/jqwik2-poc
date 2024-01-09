package jqwik2.internal.exhaustive;

import jqwik2.api.*;

public class ExhaustiveEmptyList extends ExhaustiveList {

	public ExhaustiveEmptyList() {
		super(0, null, false);
	}

	@Override
	public long maxCount() {
		return 1;
	}

	@Override
	public boolean advanceThisOrUp() {
		if (prev().isEmpty()) {
			return false;
		}
		return prev().get().advanceThisOrUp();
	}

	@Override
	public ExhaustiveSource<GenSource.List> clone() {
		return new ExhaustiveEmptyList();
	}

	@Override
	public boolean advance() {
		if (succ().isPresent()) {
			return succ().get().advance();
		}
		if (prev().isEmpty()) {
			return false;
		}
		return prev().get().advanceThisOrUp();
	}

	@Override
	public String toString() {
		return "ExhaustiveEmptyList";
	}
}
