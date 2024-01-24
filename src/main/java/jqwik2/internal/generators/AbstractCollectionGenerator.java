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
		GenSource.Tuple listSource = source.tuple(2);
		int size = chooseSize(listSource.get(0));

		List<T> elements = new ArrayList<>(size);
		GenSource.List elementsSource = listSource.get(1).list();
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
			recordings.add(tuple(atom(0), Recording.list()));
		}
		if (minSize <= 1 && maxSize >= 1) {
			elementGenerator.edgeCases().forEach(elementEdgeCase -> {
				recordings.add(tuple(atom(1), Recording.list(elementEdgeCase)));
			});
		}
		return recordings;
	}

}
