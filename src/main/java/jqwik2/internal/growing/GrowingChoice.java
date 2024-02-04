package jqwik2.internal.growing;

import jqwik2.api.*;
import jqwik2.internal.*;

class GrowingChoice extends AbstractGrowingSource implements GenSource.Choice {
	private Pair<Integer, Integer> choiceMaxAndValue = null;
	private boolean choiceRequested = false;

	@Override
	public boolean advance() {
		if (choiceMaxAndValue != null && choiceNotExhausted()) {
			advanceChoice();
			return true;
		}
		return false;
	}

	private void advanceChoice() {
		choiceMaxAndValue = new Pair<>(choiceMaxAndValue.first(), choiceMaxAndValue.second() + 1);
	}

	private boolean choiceNotExhausted() {
		return choiceMaxAndValue.second() < choiceMaxAndValue.first() - 1;
	}

	@Override
	public void reset() {
		choiceMaxAndValue = null;
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
		if (choiceMaxAndValue == null) {
			choiceMaxAndValue = new Pair<>(maxExcluded, 0);
		}
		choiceRequested = true;
		return choiceMaxAndValue.second();
	}

	@Override
	public int choose(int maxExcluded, RandomChoice.Distribution distribution) {
		return choose(maxExcluded);
	}

	@Override
	public Choice choice() {
		return this;
	}
}
