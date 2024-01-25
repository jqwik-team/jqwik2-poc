package jqwik2.internal.generators;

import jqwik2.api.*;
import jqwik2.api.arbitraries.*;

// TODO: Consider edge cases
// TODO: Cache generators
public class SourceBasedSampler implements Combinators.Sampler {
	private final GenSource.Tuple tuple;

	public SourceBasedSampler(GenSource source) {
		this.tuple = source.tuple();
	}

	@Override
	public <T> T draw(Arbitrary<T> arbitrary) {
		return generator(arbitrary).generate(tuple.nextValue());
	}

	private static <T> Generator<T> generator(Arbitrary<T> arbitrary) {
		return arbitrary.generator();
	}
}
