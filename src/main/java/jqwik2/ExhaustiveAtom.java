package jqwik2;

import jqwik2.api.*;

public class ExhaustiveAtom implements GenSource.Atom, ExhaustiveSource {

	private final int[] maxChoices;
	private int current = 0;
	private final java.util.List<ExhaustiveChoice> choices = new java.util.ArrayList<>();

	ExhaustiveAtom(int... maxChoices) {
		this.maxChoices = maxChoices;
		generateChoices();
	}

	private void generateChoices() {
		ExhaustiveChoice last = null;
		for (int maxChoice : maxChoices) {
			ExhaustiveChoice choice = new ExhaustiveChoice(maxChoice);
			choices.add(choice);
			if (last != null) {
				last.chain(choice);
			}
			last = choice;
		}
	}

	@Override
	public long maxCount() {
		if (choices.isEmpty()) {
			return 0;
		}
		return choices.getFirst().maxCount();
	}

	@Override
	public void advance() {
		choices.getLast().advance();
	}

	@Override
	public ExhaustiveSource clone() {
		return new ExhaustiveAtom(maxChoices);
	}

	@Override
	public void next() {
		current = 0;
		choices.getFirst().next();
	}

	@Override
	public void setPrev(Exhaustive exhaustive) {
		choices.getFirst().setPrev(exhaustive);
	}

	@Override
	public void setSucc(Exhaustive exhaustive) {
		choices.getLast().setSucc(exhaustive);
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
