package jqwik2.internal.growing;

import jqwik2.api.*;
import jqwik2.internal.*;

class GrowingAtom extends AbstractGrowingSource implements GenSource.Atom {
	private Pair<Integer, Integer> choice = null;
	private boolean choiceRequested = false;

	@Override
	public boolean advance() {
		if (choice != null && choiceNotExhausted()) {
			advanceChoice();
			return true;
		}
		return false;
	}

	private void advanceChoice() {
		choice = new Pair<>(choice.first(), choice.second() + 1);
	}

	private boolean choiceNotExhausted() {
		return choice.second() < choice.first() - 1;
	}

	@Override
	public void reset() {
		choice = null;
	}

	@Override
	public void next() {
		choiceRequested = false;
	}

	@Override
	public int choose(int maxExcluded) {
		if (choiceRequested) {
			throw new CannotGenerateException("no more choice available");
		}
		if (choice == null) {
			choice = new Pair<>(maxExcluded, 0);
		}
		choiceRequested = true;
		return choice.second();
	}

	@Override
	public int choose(int maxExcluded, RandomChoice.Distribution distribution) {
		return choose(maxExcluded);
	}

	@Override
	public Atom atom() {
		return this;
	}
}
