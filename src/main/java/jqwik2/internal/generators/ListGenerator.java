package jqwik2.internal.generators;

import java.util.*;
import java.util.function.*;

import jqwik2.api.*;
import jqwik2.api.recording.*;

import static jqwik2.api.recording.Recording.*;

public class ListGenerator<T> implements Generator<List<T>> {
	private final Generator<T> elementGenerator;
	private final int minSize;
	private final int maxSize;

	public ListGenerator(Generator<T> elementGenerator, int minSize, int maxSize) {
		this.elementGenerator = elementGenerator;
		this.minSize = minSize;
		this.maxSize = maxSize;
	}

	@Override
	public List<T> generate(GenSource source) {
		GenSource.Tree listSource = source.tree();
		int size = chooseSize(listSource.head());

		List<T> elements = new ArrayList<>(size);
		GenSource.List elementsSource = listSource.child().list();
		for (int i = 0; i < size; i++) {
			GenSource elementSource = elementsSource.nextElement();
			T element = elementGenerator.generate(elementSource);
			elements.add(element);
		}
		return elements;
	}

	private int chooseSize(GenSource head) {
		GenSource.Atom sizeSource = head.atom();
		int sizeRange = maxSize - minSize;
		return sizeSource.choose(sizeRange + 1) + minSize;
	}

	@Override
	public Generator<List<T>> decorate(Function<Generator<?>, Generator<?>> decorator) {
		Generator<T> decoratedElementGenerator = elementGenerator.decorate(decorator);
		return (Generator<List<T>>) decorator.apply(new ListGenerator<>(decoratedElementGenerator, minSize, maxSize));
	}

	@Override
	public Iterable<Recording> edgeCases() {
		return edgeCaseRecordings();
	}

	private Collection<Recording> edgeCaseRecordings() {
		Set<Recording> recordings = new LinkedHashSet<>();
		if (minSize == 0) {
			recordings.add(tree(atom(0), list()));
		}
		if (minSize <= 1 && maxSize >= 1) {
			elementGenerator.edgeCases().forEach(elementEdgeCase -> {
				recordings.add(tree(atom(1), list(elementEdgeCase)));
			});
		}
		return recordings;
	}

	@Override
	public Optional<ExhaustiveSource<?>> exhaustive() {
		return elementGenerator.exhaustive().flatMap(
			elementSource -> Optional.of(ExhaustiveSource.tree(
				ExhaustiveSource.atom(maxSize-minSize),
				head -> ExhaustiveSource.list(chooseSize(head), elementSource)
			)));
	}

}
