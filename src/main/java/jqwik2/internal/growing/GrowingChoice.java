package jqwik2.internal.growing;

import java.util.*;

import jqwik2.api.*;
import jqwik2.internal.*;

class GrowingChoice extends AbstractGrowingSource<GrowingChoice> implements GenSource.Choice {
	private Pair<Integer, Integer> choiceMaxAndValue = null;
	private boolean choiceRequested = false;

	GrowingChoice() {
		this(null);
	}

	public GrowingChoice(Pair<Integer, Integer> choiceMaxAndValue) {
		this.choiceMaxAndValue = choiceMaxAndValue;
	}

	private Pair<Integer, Integer> advanceChoiceAndMaxValue() {
		return new Pair<>(choiceMaxAndValue.first(), choiceMaxAndValue.second() + 1);
	}

	private boolean choiceNotExhausted() {
		return choiceMaxAndValue.second() < choiceMaxAndValue.first() - 1;
	}

	@Override
	public int choose(int maxExcluded) {
		if (choiceRequested) {
			throw new CannotGenerateException("no more choice available");
		}
		if (choiceMaxAndValue == null) {
			choiceMaxAndValue = new Pair<>(maxExcluded, 0);
		} else {
			choiceMaxAndValue = new Pair<>(maxExcluded, choiceMaxAndValue.second());
		}
		choiceRequested = true;
		return choiceMaxAndValue.second() % maxExcluded;
	}

	@Override
	public int choose(int maxExcluded, RandomChoice.Distribution distribution) {
		return choose(maxExcluded);
	}

	@Override
	public Choice choice() {
		return this;
	}

	@Override
	public Set<GrowingChoice> grow() {
		if (choiceNotExhausted()) {
			return Set.of(new GrowingChoice(advanceChoiceAndMaxValue()));
		}
		return Set.of();
	}
}
