package jqwik2.exhaustive;

import java.util.*;

import jqwik2.*;
import jqwik2.api.*;
import jqwik2.api.recording.*;
import jqwik2.exhaustive.ExhaustiveChoice.*;

public class ExhaustiveAtom extends AbstractExhaustiveSource<GenSource.Atom> {

	private final Range[] ranges;
	private final List<ExhaustiveChoice> choices = new java.util.ArrayList<>();

	public ExhaustiveAtom(int... maxChoices) {
		this(toRanges(maxChoices));
	}

	private static Range[] toRanges(int[] maxChoices) {
		return Arrays.stream(maxChoices).mapToObj((int min) -> new Range(0, min)).toArray(Range[]::new);
	}

	public ExhaustiveAtom(Range... ranges) {
		this.ranges = ranges;
		generateChoices();
	}

	private void generateChoices() {
		ExhaustiveChoice last = null;
		for (Range range : ranges) {
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
		try {
			choices.getLast().advance();
			return true;
		} catch (Generator.NoMoreValues e) {
			return false;
		}
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
		super.setSucc(exhaustive);
		choices.getLast().setSucc(exhaustive);
	}

	public Recording recording() {
		return Recording.atom(choices.stream().map(ExhaustiveChoice::fix).toList());
	}

	@Override
	public String toString() {
		return "ExhaustiveAtom{" +
				   "ranges=" + Arrays.toString(ranges) +
				   '}';
	}
}
