package jqwik2.internal.exhaustive;

import java.util.*;

import jqwik2.api.*;
import jqwik2.api.recording.*;

public class ExhaustiveAtom extends AbstractExhaustiveSource<GenSource.Atom> {

	private final ExhaustiveChoice.Range[] ranges;
	private final List<ExhaustiveChoice> choices = new java.util.ArrayList<>();

	public ExhaustiveAtom(int... maxChoices) {
		this(toRanges(maxChoices));
	}

	private static ExhaustiveChoice.Range[] toRanges(int[] maxChoices) {
		return Arrays.stream(maxChoices).mapToObj((int min) -> new ExhaustiveChoice.Range(0, min)).toArray(ExhaustiveChoice.Range[]::new);
	}

	public ExhaustiveAtom(ExhaustiveChoice.Range... ranges) {
		this.ranges = ranges;
		generateChoices();
	}

	private void generateChoices() {
		ExhaustiveChoice last = null;
		for (ExhaustiveChoice.Range range : ranges) {
			ExhaustiveChoice choice = new ExhaustiveChoice(range);
			choices.add(choice);
			if (last != null) {
				last.chain(choice);
			}
			last = choice;
		}
	}

	public int cardinality() {
		return choices.size();
	}

	@Override
	public long maxCount() {
		if (choices.isEmpty()) {
			return 0;
		}
		return choices.getFirst().maxCount();
	}

	@Override
	protected boolean tryAdvance() {
		return choices.getLast().advanceThisOrUp();
	}

	@Override
	public void reset() {
		choices.forEach(ExhaustiveChoice::reset);
	}

	@Override
	public ExhaustiveAtom clone() {
		return new ExhaustiveAtom(ranges);
	}

	@Override
	public void setSucc(Exhaustive<?> exhaustive) {
		choices.getLast().setSucc(exhaustive);
		super.setSucc(exhaustive);
	}

	public Recording recording() {
		return Recording.atom(choices.stream().map(ExhaustiveChoice::fix).toList());
	}

	@Override
	public String toString() {
		return "ExhaustiveAtom{ranges=%s, recording=%s}".formatted(Arrays.toString(ranges), recording());
	}
}
