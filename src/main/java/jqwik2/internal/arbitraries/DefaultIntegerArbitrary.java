package jqwik2.internal.arbitraries;

import java.util.*;

import jqwik2.api.*;
import jqwik2.api.arbitraries.*;
import jqwik2.internal.generators.*;

public class DefaultIntegerArbitrary implements IntegerArbitrary {

	private final int min;
	private final int max;

	public DefaultIntegerArbitrary() {
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

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;

		DefaultIntegerArbitrary that = (DefaultIntegerArbitrary) o;

		if (min != that.min) return false;
		return max == that.max;
	}

	@Override
	public int hashCode() {
		return Objects.hash(min, max);
	}
}
