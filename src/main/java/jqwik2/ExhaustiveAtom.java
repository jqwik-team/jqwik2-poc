package jqwik2;

import jqwik2.api.*;

public class ExhaustiveAtom extends AbstractExhaustive implements GenSource.Atom {

	private int current = 0;
	private java.util.List<ExhaustiveChoice> choices = new java.util.ArrayList<>();

	public ExhaustiveAtom(int... maxChoices) {
		generateChoices(maxChoices);
	}

	private void generateChoices(int[] maxChoices) {
		ExhaustiveChoice last = null;
		for (int maxChoice : maxChoices) {
			ExhaustiveChoice choice = new ExhaustiveChoice(maxChoice);
			choices.add(choice);
			if (succ == null) {
				succ = choice;
			} else {
				if (last != null) {
					last.chain(choice);
				}
			}
			last = choice;
		}
	}

	@Override
	public long maxCount() {
		if (succ == null) {
			return 0;
		}
		return succ.maxCount();
	}

	@Override
	public void advance() {
		if (prev != null) {
			reset();
			prev.advance();
		} else {
			Generator.noMoreValues();
		}
	}

	@Override
	public void next() {
		reset();
		super.next();
	}

	private void reset() {
		current = 0;
	}

	@Override
	public Atom atom() {
		return this;
	}

	@Override
	public List list() {
		throw new CannotGenerateException("Source is not a list");
	}

	@Override
	public Tree tree() {
		throw new CannotGenerateException("Source is not a tree");
	}

	@Override
	public int choose(int maxExcluded) {
		if (current >= choices.size()) {
			throw new CannotGenerateException("No more choices!");
		}
		ExhaustiveChoice currentChoice = choices.get(current++);
		return currentChoice.choose(maxExcluded);
	}

}
