package jqwik2.api.arbitraries;

import jqwik2.api.*;
import jqwik2.internal.*;
import jqwik2.internal.generators.*;

public interface IntegerArbitrary extends Arbitrary<Integer> {
	IntegerArbitrary between(int min, int max);
}

class DefaultIntegerArbitrary implements IntegerArbitrary {

	private final int min;
	private final int max;

	DefaultIntegerArbitrary() {
		this(Integer.MIN_VALUE, Integer.MAX_VALUE);
	}

	private DefaultIntegerArbitrary(int min, int max) {
		this.min = min;
		this.max = max;
	}

	@Override
	public IntegerArbitrary between(int min, int max) {
		return new DefaultIntegerArbitrary(min, max);
	}

	@Override
	public Generator<Integer> generator() {
		return BaseGenerators.integers(min, max, RandomChoice.Distribution.biased(5));
	}
}