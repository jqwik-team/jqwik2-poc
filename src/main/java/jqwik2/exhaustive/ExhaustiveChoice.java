package jqwik2.exhaustive;

public class ExhaustiveChoice extends AbstractExhaustive<ExhaustiveChoice> {

	private final Range range;
	private int currentValue = 0;

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
		currentValue = range.min;
	}

	public int choose(int maxExcluded) {
		return currentValue % maxExcluded;
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
		if (currentValue < range.max) {
			currentValue++;
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
		return "ExhaustiveChoice(range=%s, current=%d)".formatted(range, currentValue);
	}

	public Integer fix() {
		return currentValue;
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
