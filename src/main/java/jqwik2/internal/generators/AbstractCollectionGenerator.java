package jqwik2.internal.generators;

import java.util.*;

import jqwik2.api.*;
import jqwik2.api.recording.*;

import static jqwik2.api.recording.Recording.*;

abstract class AbstractCollectionGenerator<T, C> implements Generator<C> {
	protected final Generator<T> elementGenerator;
	protected final int minSize;
	protected final int maxSize;
	protected final Set<FeatureExtractor<T>> featureExtractors;

	AbstractCollectionGenerator(Generator<T> elementGenerator, int minSize, int maxSize, Set<FeatureExtractor<T>> featureExtractors) {
		this.elementGenerator = elementGenerator;
		this.minSize = minSize;
		this.maxSize = maxSize;

		this.featureExtractors = featureExtractors;
	}

	Collection<T> generateCollection(GenSource source) {
		GenSource.Tree listSource = source.tree();
		int size = chooseSize(listSource.head());

		List<T> elements = new ArrayList<>(size);
		GenSource.List elementsSource = listSource.child().list();
		for (int i = 0; i < size; i++) {
			GenSource elementSource = elementsSource.nextElement();
			while (true) {
				T element = elementGenerator.generate(elementSource);
				boolean elementIsUniqueWithRegardToExtractors =
					featureExtractors.stream().allMatch(
						extractor -> extractor.isUniqueIn(element, elements)
					);
				if (elementIsUniqueWithRegardToExtractors) {
					elements.add(element);
					break;
				}
			}
		}
		return elements;
	}

	protected int chooseSize(GenSource head) {
		GenSource.Atom sizeSource = head.atom();
		int sizeRange = maxSize - minSize;
		return sizeSource.choose(sizeRange + 1) + minSize;
	}

	@Override
	public Iterable<Recording> edgeCases() {
		return edgeCaseRecordings();
	}

	private Collection<Recording> edgeCaseRecordings() {
		Set<Recording> recordings = new LinkedHashSet<>();
		if (minSize == 0) {
			recordings.add(tree(atom(0), Recording.list()));
		}
		if (minSize <= 1 && maxSize >= 1) {
			elementGenerator.edgeCases().forEach(elementEdgeCase -> {
				recordings.add(tree(atom(1), Recording.list(elementEdgeCase)));
			});
		}
		return recordings;
	}

	// @Override
	// public Optional<? extends ExhaustiveSource<?>> exhaustive() {
	// 	return ExhaustiveSource.tree(
	// 		ExhaustiveSource.atom(maxSize - minSize),
	// 		head -> ExhaustiveSource.list(chooseSize(head), elementGenerator.exhaustive())
	// 	);
	// }

}
