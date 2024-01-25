package jqwik2.internal.generators;

import java.util.*;
import java.util.function.*;

import jqwik2.api.*;
import jqwik2.api.arbitraries.*;

class CombineGenerator<T> implements Generator<T> {

	private final Function<Combinators.Sampler, T> combinator;
	private final List<DecoratorFunction> decorators;
	private SamplerSupplier samplerSupplier;

	public CombineGenerator(Function<Combinators.Sampler, T> combinator) {
		this(combinator, List.of());
	}

	public CombineGenerator(Function<Combinators.Sampler, T> combinator, List<DecoratorFunction> decorators) {
		this.combinator = combinator;
		this.decorators = decorators;
	}

	@Override
	public Generator<T> decorate(DecoratorFunction decorator) {
		List<DecoratorFunction> newDecorators = new ArrayList<>(decorators);
		newDecorators.add(decorator);
		return new CombineGenerator<>(combinator, newDecorators);
	}

	@Override
	public T generate(GenSource source) {
		if (samplerSupplier == null) {
			samplerSupplier = new SamplerSupplier(decorators);
		}
		return combinator.apply(samplerSupplier.get(source.tuple()));
	}
}
