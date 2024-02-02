package jqwik2.internal.growing;

import java.util.*;

import jqwik2.api.*;
import jqwik2.internal.*;

class GrowingAtom implements GrowingSource, GenSource.Atom {
	private final java.util.List<Pair<Integer, Integer>> choices = new ArrayList<>();
	private int currentChoiceIndex = 0;

	public boolean advance() {
		this.currentChoiceIndex = 0;
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
	public int choose(int maxExcluded) {
		if (choices.size() < currentChoiceIndex + 1) {
			choices.add(new Pair<>(maxExcluded, 0));
		}
		var currentChoice = choices.get(currentChoiceIndex++).second();
		// System.out.printf("choose(%d)=%d%n", maxExcluded, currentChoice);
		return currentChoice;
	}

	@Override
	public int choose(int maxExcluded, RandomChoice.Distribution distribution) {
		return choose(maxExcluded);
	}

	public void reset() {
		currentChoiceIndex = 0;
		choices.clear();
	}

	@Override
	public Atom atom() {
		return this;
	}

	@Override
	public List list() {
		throw new CannotGenerateException("Cannot generate list from atom");
	}

	@Override
	public Tuple tuple() {
		throw new CannotGenerateException("Cannot generate tuple from atom");
	}
}
