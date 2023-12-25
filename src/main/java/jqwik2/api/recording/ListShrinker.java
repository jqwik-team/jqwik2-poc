package jqwik2.api.recording;

import java.util.*;
import java.util.stream.*;

class ListShrinker {
	private final List<Recording> elements;

	ListShrinker(ListRecording treeRecording) {
		this.elements = treeRecording.elements();
	}

	Stream<ListRecording> shrink() {
		if (elements.isEmpty()) {
			return Stream.empty();
		}
		return Stream.concat(
			shrinkIndividually(),
			reorder()
		).map(ListRecording::new);
	}

	private Stream<List<Recording>> shrinkIndividually() {
		return IntStream.range(0, elements.size() - 1)
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

	private Stream<List<Recording>> reorder() {
		List<Recording> reorderedChildren = new ArrayList<>(elements);
		Collections.sort(reorderedChildren);
		return Stream.of(reorderedChildren);
	}
}
