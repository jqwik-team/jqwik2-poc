package jqwik2.internal.growing;

import java.util.*;

import jqwik2.api.*;
import jqwik2.internal.*;

public class GrowingGenSource implements GrowingSource {

	private GrowingSource currentSource;
	private boolean resourceRequested = false;

	@Override
	public Atom atom() {
		if (resourceRequested) {
			throw new CannotGenerateException("Already requested a resource");
		}
		if (currentSource == null) {
			currentSource = new GrowingAtom();
		} else if (! (currentSource instanceof GrowingAtom)) {
			throw new CannotGenerateException("Source is not an atom");
		}
		resourceRequested = true;
		return (Atom) currentSource;
	}

	@Override
	public List list() {
		return null;
	}

	@Override
	public Tuple tuple() {
		return null;
	}

	public boolean advance() {
		resourceRequested = false;
		if (currentSource == null) {
			return false;
		}
		return currentSource.advance();
	}

	public void reset() {
		if (currentSource == null) {
			return;
		}
		currentSource.reset();
	}

	private static class GrowingAtom implements GrowingSource, Atom {
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

}
