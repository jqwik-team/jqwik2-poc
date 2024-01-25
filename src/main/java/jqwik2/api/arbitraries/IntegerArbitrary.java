package jqwik2.api.arbitraries;

import jqwik2.api.*;

public interface IntegerArbitrary extends Arbitrary<Integer> {
	IntegerArbitrary between(int min, int max);
}

