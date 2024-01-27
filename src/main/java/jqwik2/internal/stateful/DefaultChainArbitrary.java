package jqwik2.internal.stateful;

import java.util.*;
import java.util.function.*;

import jqwik2.api.*;
import jqwik2.api.stateful.*;
import jqwik2.internal.*;

public class DefaultChainArbitrary<T> implements ChainArbitrary<T> {

	private int maxTransformations;
	private List<Pair<Integer, Transformation<T>>> weightedTransformations;
	private final Supplier<? extends T> initialSupplier;

	public DefaultChainArbitrary(Supplier<? extends T> initialSupplier) {
		this(initialSupplier, List.of(), Integer.MIN_VALUE);
	}

	private DefaultChainArbitrary(
		Supplier<? extends T> initialSupplier,
		List<Pair<Integer, Transformation<T>>> weightedTransformations,
		int maxTransformations
	) {
		this.initialSupplier = initialSupplier;
		this.weightedTransformations = weightedTransformations;
		this.maxTransformations = maxTransformations;
	}

	@Override
	public Generator<Chain<T>> generator() {
		final int effectiveMaxTransformations =
			this.maxTransformations != Integer.MIN_VALUE ? this.maxTransformations : (int) Math.max(Math.round(Math.sqrt(JqwikDefaults.defaultMaxTries())), 10);

		return new ChainGenerator<>(initialSupplier, weightedTransformations, effectiveMaxTransformations);
	}

	@Override
	public ChainArbitrary<T> withTransformation(int weight, Transformation<T> transformation) {
		if (weight <= 0) {
			throw new IllegalArgumentException("Weight for transformation must be >= 1");
		}
		List<Pair<Integer, Transformation<T>>> newWeightedTransformations = new ArrayList<>(weightedTransformations);
		newWeightedTransformations.add(new Pair<>(weight, transformation));
		return new DefaultChainArbitrary<>(initialSupplier, newWeightedTransformations, maxTransformations);
	}

	@Override
	public ChainArbitrary<T> withMaxTransformations(int maxTransformations) {
		return new DefaultChainArbitrary<>(initialSupplier, weightedTransformations, maxTransformations);
	}
}
