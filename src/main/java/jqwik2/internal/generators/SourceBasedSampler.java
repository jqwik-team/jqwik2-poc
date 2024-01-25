package jqwik2.internal.generators;

import java.util.*;

import jqwik2.api.*;
import jqwik2.api.arbitraries.*;

// TODO: Cache generators
public class SourceBasedSampler implements Combinators.Sampler {
	private final GenSource.Tuple tuple;
	private final List<Generator.DecoratorFunction> decorators;

	public SourceBasedSampler(GenSource source, List<Generator.DecoratorFunction> decorators) {
		this.tuple = source.tuple();
		this.decorators = decorators;
	}

	@Override
	public <T> T draw(Arbitrary<T> arbitrary) {
		return generator(arbitrary).generate(tuple.nextValue());
	}

	@SuppressWarnings("unchecked")
	private <T> Generator<T> generator(Arbitrary<T> arbitrary) {
		Generator<?> generator = arbitrary.generator();
		for (Generator.DecoratorFunction decorator : decorators) {
			generator = decorator.apply(generator);
		}
		return (Generator<T>) generator;
	}
}
