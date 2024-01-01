package jqwik2.exhaustive;

import java.util.*;

import jqwik2.*;
import jqwik2.exhaustive.ExhaustiveChoice.*;
import jqwik2.api.*;
import jqwik2.api.recording.*;

public class ExhaustiveAtom implements GenSource.Atom, ExhaustiveSource {

	private final Range[] ranges;
	private int current = 0;
	private final java.util.List<ExhaustiveChoice> choices = new java.util.ArrayList<>();

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
	public void advance() {
		choices.getLast().advance();
	}

	@Override
	public ExhaustiveAtom clone() {
		return new ExhaustiveAtom(ranges);
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

	public Atom fix() {
		AtomRecording recording = Recording.atom(choices.stream().map(ExhaustiveChoice::fix).toList());
		return new RecordedSource(recording);
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

	@Override
	public String toString() {
		return "ExhaustiveAtom{" +
				   "ranges=" + Arrays.toString(ranges) +
				   '}';
	}
}
