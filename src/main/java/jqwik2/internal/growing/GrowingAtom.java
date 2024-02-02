package jqwik2.internal.growing;

import java.util.*;

import jqwik2.api.*;
import jqwik2.internal.*;

class GrowingAtom extends AbstractGrowingSource implements GenSource.Atom {
	private final java.util.List<Pair<Integer, Integer>> choices = new ArrayList<>();
	private int currentChoiceIndex = 0;

	@Override
	public boolean advance() {
		for (int index = choices.size() - 1; index >= 0; index--) {
			var choice = choices.get(index);
			if (choiceNotExhausted(choice)) {
				advanceChoice(index, choice);
				clearChoicesAfter(index);
				return true;
			}
		}
		return false;
	}

	private void clearChoicesAfter(int index) {
		choices.subList(index + 1, choices.size()).clear();
	}

	private void advanceChoice(int index, Pair<Integer, Integer> choice) {
		choices.set(index, new Pair<>(choice.first(), choice.second() + 1));
	}

	private static boolean choiceNotExhausted(Pair<Integer, Integer> choice) {
		return choice.second() < choice.first() - 1;
	}

	@Override
	public void reset() {
		choices.clear();
	}

	@Override
	public void next() {
		currentChoiceIndex = 0;
	}

	@Override
	public int choose(int maxExcluded) {
		if (choices.size() < currentChoiceIndex + 1) {
			choices.add(new Pair<>(maxExcluded, 0));
		}
		return choices.get(currentChoiceIndex++).second();
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
