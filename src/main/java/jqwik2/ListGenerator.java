package jqwik2;

import java.util.*;
import java.util.function.*;

import jqwik2.api.*;
import jqwik2.api.recording.*;
import jqwik2.exhaustive.*;

import static jqwik2.api.recording.Recording.*;

public class ListGenerator<T> implements Generator<List<T>> {
	private final Generator<T> elementGenerator;
	private final int maxSize;

	public ListGenerator(Generator<T> elementGenerator, int maxSize) {
		this.elementGenerator = elementGenerator;
		this.maxSize = maxSize;
	}

	@Override
	public List<T> generate(GenSource source) {
		GenSource.Tree listSource = source.tree();
		GenSource.Atom sizeSource = listSource.head().atom();

		int size = sizeSource.choose(maxSize + 1);

		List<T> elements = new ArrayList<>(size);
		GenSource.List elementsSource = listSource.child().list();
		for (int i = 0; i < size; i++) {
			GenSource elementSource = elementsSource.nextElement();
			T element = elementGenerator.generate(elementSource);
			elements.add(element);
		}
		return elements;
	}

	@Override
	public Generator<List<T>> decorate(Function<Generator<?>, Generator<?>> decorator) {
		Generator<T> decoratedElementGenerator = elementGenerator.decorate(decorator);
		return (Generator<List<T>>) decorator.apply(new ListGenerator<>(decoratedElementGenerator, maxSize));
	}

	@Override
	public Iterable<Recording> edgeCases() {
		return edgeCaseRecordings();
	}

	private Collection<Recording> edgeCaseRecordings() {
		Set<Recording> recordings = new LinkedHashSet<>();
		recordings.add(tree(atom(0), list()));
		elementGenerator.edgeCases().forEach(elementEdgeCase -> {
			recordings.add(tree(atom(1), list(elementEdgeCase)));
		});
		return recordings;
	}

	@Override
	public Optional<ExhaustiveSource<?>> exhaustive() {
		return elementGenerator.exhaustive().flatMap(
			elementSource -> Optional.of(ExhaustiveSource.tree(
				new ExhaustiveChoice.Range(0, maxSize),
				size -> ExhaustiveSource.list(size, elementSource)
			)));
	}

}
