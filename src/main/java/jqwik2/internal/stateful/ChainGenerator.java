package jqwik2.internal.stateful;

import java.util.*;
import java.util.function.*;

import jqwik2.api.*;
import jqwik2.api.stateful.*;
import jqwik2.internal.*;
import jqwik2.internal.generators.*;

class ChainGenerator<S> implements Generator<Chain<S>> {
	private final Supplier<? extends S> initialSupplier;
	private final int maxTransformations;
	private final Generator<Transformation<S>> transformationGenerator;

	ChainGenerator(
		Supplier<? extends S> initialSupplier,
		List<Pair<Integer, Transformation<S>>> weightedTransformations,
		int maxTransformations
	) {
		this.initialSupplier = initialSupplier;
		this.maxTransformations = maxTransformations;
		this.transformationGenerator = BaseGenerators.frequency(weightedTransformations);
	}

	@Override
	public Chain<S> generate(GenSource source) {
		return new ChainInstance(initialSupplier, maxTransformations, transformationGenerator, source.list());
	}

}
