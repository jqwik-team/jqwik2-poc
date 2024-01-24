package jqwik2.api.recording;

import java.util.*;
import java.util.stream.*;

class TupleShrinker {
	private final List<Recording> elements;

	TupleShrinker(TupleRecording tupleRecording) {
		this.elements = tupleRecording.elements();
	}

	// TODO: Shrink two or more elements together
	// TODO: Shrink list of lists by moving element of inner list to next list element
	// TODO: Shrink tuples that represent a list by shrinking first element (size) and then second element (list)
	Stream<TupleRecording> shrink() {
		if (elements.isEmpty()) {
			return Stream.empty();
		}
		return shrinkIndividually().map(TupleRecording::new);
	}

	private Stream<List<Recording>> shrinkIndividually() {
		return IntStream.range(0, elements.size())
						.boxed()
						.flatMap(this::shrinkElement);
	}

	private Stream<List<Recording>> shrinkElement(int index) {
		Recording child = elements.get(index);
		return child.shrink().map(shrunkChild -> replaceElement(index, shrunkChild));
	}

	private List<Recording> replaceElement(int index, Recording shrunkChild) {
		List<Recording> shrunkChildren = new ArrayList<>(elements);
		shrunkChildren.set(index, shrunkChild);
		return shrunkChildren;
	}
}
