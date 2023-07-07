package jqwik2;

public class IntegerGenerator implements ValueGenerator<Integer> {

	private final int min;
	private final int max;

	public IntegerGenerator() {
		this(Integer.MIN_VALUE, Integer.MAX_VALUE);
	}

	public IntegerGenerator(int min, int max) {
		this.min = min;
		this.max = max;
	}

	@Override
	public GeneratedValue<Integer> generate(GenerationSource source) {
		return new GeneratedValue<>(source.next(1, min, max)[0]);
	}
}
