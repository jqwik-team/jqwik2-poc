package jqwik2.api.arbitraries;

import jqwik2.api.*;
import jqwik2.internal.*;
import jqwik2.internal.generators.*;

public class Numbers {

	private Numbers() {}

	public static Arbitrary<Integer> integers() {
		return new Arbitrary<Integer>() {
			@Override
			public Generator<Integer> generator() {
				Generator<Integer> integerGenerator = BaseGenerators.integers(Integer.MIN_VALUE, Integer.MAX_VALUE, RandomChoice.Distribution.biased(5));
				return WithEdgeCasesDecorator.decorate(integerGenerator, 0.05, 100);
			}
		};
	}
}
