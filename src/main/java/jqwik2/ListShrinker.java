package jqwik2;

import java.util.*;
import java.util.stream.*;

public class ListShrinker {
	private final List<ChoicesRecording> elements;

	public ListShrinker(ListRecording treeRecording) {
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

	private Stream<List<ChoicesRecording>> shrinkIndividually() {
		return IntStream.range(0, elements.size() - 1)
						.boxed()
						.flatMap(this::shrinkElement);
	}

	private Stream<List<ChoicesRecording>> shrinkElement(int index) {
		ChoicesRecording child = elements.get(index);
		return child.shrink().map(shrunkChild -> replaceElement(index, shrunkChild));
	}

	private List<ChoicesRecording> replaceElement(int index, ChoicesRecording shrunkChild) {
		List<ChoicesRecording> shrunkChildren = new ArrayList<>(elements);
		shrunkChildren.set(index, shrunkChild);
		return shrunkChildren;
	}

	private Stream<List<ChoicesRecording>> reorder() {
		List<ChoicesRecording> reorderedChildren = new ArrayList<>(elements);
		Collections.sort(reorderedChildren);
		return Stream.of(reorderedChildren);
	}
}
