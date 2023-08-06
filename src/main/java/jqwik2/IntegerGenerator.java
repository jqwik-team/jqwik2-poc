package jqwik2;

import java.util.*;

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
		int[] values = source.next(1, min, max);
		int value = values[0];
		return new GeneratedValue<>(value, this, new BaseSeed(1, min, max, values), List.of(value));
	}
}
