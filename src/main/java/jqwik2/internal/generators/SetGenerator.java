package jqwik2.internal.generators;

import java.util.*;

import jqwik2.api.*;

public class SetGenerator<T> extends AbstractCollectionGenerator<T, Set<T>> {

	public SetGenerator(Generator<T> elementGenerator, int minSize, int maxSize) {
		super(elementGenerator, minSize, maxSize, Collections.singleton(FeatureExtractor.identity()));
	}

	@Override
	public Set<T> generate(GenSource source) {
		return new LinkedHashSet<>(generateCollection(source));
	}

	@Override
	public Optional<ExhaustiveSource<?>> exhaustive() {
		return ExhaustiveSource.tree(
			ExhaustiveSource.atom(maxSize - minSize),
			head -> ExhaustiveSource.set(chooseSize(head), elementGenerator.exhaustive())
		);
	}

}
