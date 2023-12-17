package jqwik2gen;

import java.util.*;
import java.util.stream.*;

public class ListShrinker {
	private final List<SourceRecording> elements;

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

	private Stream<List<SourceRecording>> shrinkIndividually() {
		return IntStream.range(0, elements.size() - 1)
						.boxed()
						.flatMap(this::shrinkElement);
	}

	private Stream<List<SourceRecording>> shrinkElement(int index) {
		SourceRecording child = elements.get(index);
		return child.shrink().map(shrunkChild -> replaceElement(index, shrunkChild));
	}

	private List<SourceRecording> replaceElement(int index, SourceRecording shrunkChild) {
		List<SourceRecording> shrunkChildren = new ArrayList<>(elements);
		shrunkChildren.set(index, shrunkChild);
		return shrunkChildren;
	}

	private Stream<List<SourceRecording>> reorder() {
		List<SourceRecording> reorderedChildren = new ArrayList<>(elements);
		Collections.sort(reorderedChildren);
		return Stream.of(reorderedChildren);
	}
}
