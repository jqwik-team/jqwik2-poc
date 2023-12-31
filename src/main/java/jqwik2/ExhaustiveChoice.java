package jqwik2;

import jqwik2.api.*;

public class ExhaustiveChoice implements Exhaustive<ExhaustiveChoice> {

	private final Range range;
	private int current = 0;
	private Exhaustive<?> succ = null;
	private Exhaustive<?> prev = null;

	public ExhaustiveChoice(int maxExcluded) {
		this(0, maxExcluded);
	}

	public ExhaustiveChoice(int min, int maxExcluded) {
		this(new Range(min, maxExcluded));
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
		if (current < range.maxExcluded) {
			current++;
		}
		if (current == range.maxExcluded) {
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

	public record Range(int min, int maxExcluded) {
		public int size() {
			return maxExcluded - min;
		}

		@Override
		public String toString() {
			return "[%d-%d[".formatted(min, maxExcluded);
		}
	}
}
