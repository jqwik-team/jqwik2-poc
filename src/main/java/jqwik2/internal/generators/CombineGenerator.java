package jqwik2.internal.generators;

import java.util.function.*;

import jqwik2.api.*;
import jqwik2.api.arbitraries.*;

class CombineGenerator<T> implements Generator<T> {

	private final Function<Combinators.Sampler, T> combinator;

	public CombineGenerator(Function<Combinators.Sampler, T> combinator) {
		this.combinator = combinator;
	}

	@Override
	public T generate(GenSource source) {
		Combinators.Sampler sampler = new SourceBasedSampler(source);
		return combinator.apply(sampler);
	}
}
