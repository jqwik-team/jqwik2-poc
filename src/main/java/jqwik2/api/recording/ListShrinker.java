package jqwik2.api.recording;

import java.util.*;
import java.util.stream.*;

import jqwik2.internal.*;

class ListShrinker {
	private final List<Recording> elements;

	ListShrinker(ListRecording listRecording) {
		this.elements = listRecording.elements();
	}

	// TODO: Shrink two or more elements together
	// TODO: Shrink list of lists by moving element of inner list to next list element
	Stream<ListRecording> shrink() {
		if (elements.isEmpty()) {
			return Stream.empty();
		}
		return StreamConcatenation.concat(
			shrinkIndividually(),
			reorder(),
			discardElements()
		).map(ListRecording::new);
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

	private Stream<List<Recording>> reorder() {
		var reorderedChildren = sortAscending(elements);
		if (reorderedChildren.equals(elements)) {
			return Stream.empty();
		}
		return Stream.of(reorderedChildren);
	}

	private List<Recording> sortAscending(List<Recording> recordings) {
		List<Recording> reorderedChildren = new ArrayList<>(recordings);
		Collections.sort(reorderedChildren);
		return reorderedChildren;
	}

	private Stream<List<Recording>> discardElements() {
		return IntStream.range(0, elements.size())
						.boxed()
						.flatMap(this::discardElement);
	}

	private Stream<? extends List<Recording>> discardElement(int index) {
		List<Recording> shrunkChildren = new ArrayList<>(elements);
		shrunkChildren.remove(index);
		return Stream.of(shrunkChildren);
	}

}
