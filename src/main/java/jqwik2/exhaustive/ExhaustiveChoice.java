package jqwik2.exhaustive;

import jqwik2.api.*;

public class ExhaustiveChoice implements Exhaustive<ExhaustiveChoice> {

	private final Range range;
	private int current = 0;
	private Exhaustive<?> succ = null;
	private Exhaustive<?> prev = null;

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
	public void next() {
		if (succ == null) {
			advance();
		} else {
			succ.next();
		}
	}

	public int choose(int maxExcluded) {
		return current % maxExcluded;
	}

	public void reset() {
		current = range.min;
	}

	@Override
	public long maxCount() {
		int localMaxCount = range.size();
		if (succ != null) {
			return localMaxCount * succ.maxCount();
		}
		return localMaxCount;
	}

	@Override
	public void setSucc(Exhaustive<?> exhaustive) {
		this.succ = exhaustive;
	}

	@Override
	public void setPrev(Exhaustive<?> exhaustive) {
		this.prev = exhaustive;
	}

	@Override
	public void advance() {
		if (current <= range.max) {
			current++;
		}
		if (current > range.max) {
			reset();
			if (prev != null) {
				prev.advance();
			} else {
				Generator.noMoreValues();
			}
		}
	}

	@Override
	public ExhaustiveChoice clone() {
		return new ExhaustiveChoice(range);
	}

	@Override
	public String toString() {
		return "ExhaustiveChoice(range=%d, current=%d)".formatted(range, current);
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
