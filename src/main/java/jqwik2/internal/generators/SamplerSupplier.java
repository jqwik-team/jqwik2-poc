package jqwik2.internal.generators;

import java.util.*;
import java.util.concurrent.*;

import jqwik2.api.*;
import jqwik2.api.arbitraries.*;

class SamplerSupplier {
	private final List<Generator.DecoratorFunction> decorators;
	private final Map<Arbitrary<?>, Generator<?>> generatorCache = new ConcurrentHashMap<>();

	SamplerSupplier(List<Generator.DecoratorFunction> decorators) {
		this.decorators = decorators;
	}

	Combinators.Sampler get(GenSource.Tuple tuple) {
		return new Combinators.Sampler() {
			@Override
			public <T> T draw(Arbitrary<T> arbitrary) {
				return generator(arbitrary).generate(tuple.nextValue());
			}
		};
	}

	@SuppressWarnings("unchecked")
	private <T> Generator<T> generator(Arbitrary<T> arbitrary) {
		var generator = generatorCache.computeIfAbsent(arbitrary, this::createGenerator);
		return (Generator<T>) generator;
	}

	private Generator<?> createGenerator(Arbitrary<?> arbitrary) {
		Generator<?> generator = arbitrary.generator();
		for (Generator.DecoratorFunction decorator : decorators) {
			generator = decorator.apply(generator);
		}
		return generator;
	}
}
