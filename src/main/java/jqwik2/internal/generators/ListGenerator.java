package jqwik2.internal.generators;

import java.util.*;

import jqwik2.api.*;

public class ListGenerator<T> extends AbstractCollectionGenerator<T, List<T>> {

	public ListGenerator(Generator<T> elementGenerator, int minSize, int maxSize) {
		super(elementGenerator, minSize, maxSize, Collections.emptySet());
	}

	@Override
	public List<T> generate(GenSource source) {
		return new ArrayList<>(generateCollection(source));
	}

	@SuppressWarnings("unchecked")
	@Override
	public Generator<List<T>> decorate(DecoratorFunction decorator) {
		Generator<T> decoratedElementGenerator = elementGenerator.decorate(decorator);
		return (Generator<List<T>>) decorator.apply(new ListGenerator<>(decoratedElementGenerator, minSize, maxSize));
	}

	@Override
	public Optional<ExhaustiveSource<?>> exhaustive() {
		return ExhaustiveSource.flatMap(
			ExhaustiveSource.choice(maxSize - minSize),
			head -> ExhaustiveSource.list(chooseSize(head), elementGenerator.exhaustive())
		);
	}

}
