package jqwik2.exhaustive;

import jqwik2.api.*;

public class ExhaustiveChoice extends AbstractExhaustive<ExhaustiveChoice> {

	private final Range range;
	private int current = 0;

	public ExhaustiveChoice(int maxIncluded) {
		this(0, maxIncluded);
	}

	public ExhaustiveChoice(int min, int maxIncluded) {
		this(new Range(min, maxIncluded));
	}

	public ExhaustiveChoice(Range range) {
		this.range = range;
		reset();
	}

	@Override
	public void reset() {
		current = range.min;
	}

	public int choose(int maxExcluded) {
		return current % maxExcluded;
	}

	@Override
	public long maxCount() {
		int localMaxCount = range.size();
		if (succ().isPresent()) {
			return localMaxCount * succ().get().maxCount();
		}
		return localMaxCount;
	}

	@Override
	public void advance() {
		if (tryAdvance()) {
			return;
		}
		reset();
		if (prev().isPresent()) {
			prev().get().advance();
		} else {
			Generator.noMoreValues();
		}
	}

	private boolean tryAdvance() {
		if (current < range.max) {
			current++;
			return true;
		} else {
			return false;
		}
	}

	@Override
	public ExhaustiveChoice clone() {
		return new ExhaustiveChoice(range);
	}

	@Override
	public String toString() {
		return "ExhaustiveChoice(range=%s, current=%d)".formatted(range, current);
	}

	public Integer fix() {
		return current;
	}

	public record Range(int min, int max) {
		public int size() {
			return (max - min) + 1;
		}

		@Override
		public String toString() {
			return "[%d-%d]".formatted(min, max);
		}
	}
}
