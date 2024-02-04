package jqwik2.internal.exhaustive;

import jqwik2.api.*;
import jqwik2.api.recording.*;

public class ExhaustiveChoice extends AbstractExhaustiveSource<GenSource.Choice> {

	private final Range range;
	private int currentValue = 0;

	public ExhaustiveChoice(int maxChoiceIncluded) {
		this(toRange(maxChoiceIncluded));
	}

	private static Range toRange(int maxChoice) {
		return new Range(0, maxChoice);
	}

	public ExhaustiveChoice(Range includedRange) {
		this.range = includedRange;
		reset();
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
	protected boolean tryAdvance() {
		if (currentValue >= range.max()) {
			return false;
		}
		currentValue++;
		return true;
	}

	@Override
	public void reset() {
		currentValue = range.min();
	}

	@Override
	public ExhaustiveChoice clone() {
		return new ExhaustiveChoice(range);
	}

	public Recording recording() {
		return Recording.choice(currentValue);
	}

	@Override
	public String toString() {
		return "ExhaustiveChoice{range=%s, recording=%s}".formatted(range, recording());
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
