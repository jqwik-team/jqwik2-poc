package jqwik2.internal.exhaustive;

import jqwik2.api.*;
import jqwik2.api.recording.*;

public class ExhaustiveEmptyCollection extends AbstractExhaustiveSource<GenSource.List>{

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
	protected boolean tryAdvance() {
		return false;
	}

	@Override
	public void reset() {

	}

	@Override
	public ExhaustiveEmptyCollection clone() {
		return new ExhaustiveEmptyCollection();
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
		return "ExhaustiveEmptyCollection";
	}

	@Override
	public Recording recording() {
		return Recording.list();
	}
}
