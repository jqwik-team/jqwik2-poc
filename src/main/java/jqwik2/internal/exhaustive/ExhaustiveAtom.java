package jqwik2.internal.exhaustive;

import jqwik2.api.*;
import jqwik2.api.recording.*;

public class ExhaustiveAtom extends AbstractExhaustiveSource<GenSource.Atom> {

	private final ExhaustiveChoice.Range range;
	private ExhaustiveChoice choice;

	public ExhaustiveAtom(int maxChoiceIncluded) {
		this(toRange(maxChoiceIncluded));
	}

	private static ExhaustiveChoice.Range toRange(int maxChoice) {
		return new ExhaustiveChoice.Range(0, maxChoice);
	}

	public ExhaustiveAtom(ExhaustiveChoice.Range includedRange) {
		this.range = includedRange;
		this.choice = new ExhaustiveChoice(range);
	}

	@Override
	public long maxCount() {
		return choice.maxCount();
	}

	@Override
	protected boolean tryAdvance() {
		return choice.advanceThisOrUp();
	}

	@Override
	public void reset() {
		choice.reset();
	}

	@Override
	public ExhaustiveAtom clone() {
		return new ExhaustiveAtom(range);
	}

	@Override
	public void setSucc(Exhaustive<?> exhaustive) {
		choice.setSucc(exhaustive);
		super.setSucc(exhaustive);
	}

	public Recording recording() {
		return Recording.atom(choice.fix());
	}

	@Override
	public String toString() {
		return "ExhaustiveAtom{range=%s, recording=%s}".formatted(range, recording());
	}
}
