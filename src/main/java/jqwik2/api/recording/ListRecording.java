package jqwik2.api.recording;

import java.util.*;
import java.util.stream.*;

public record ListRecording(List<Recording> elements) implements Recording {

	@Override
	public Stream<? extends Recording> shrink() {
		return new ListShrinker(this).shrink();
	}

	@Override
	public int compareTo(Recording other) {
		if (other instanceof ListRecording otherList) {
			List<Recording> mySortedChildren = new ArrayList<>(this.elements);
			Collections.sort(mySortedChildren);
			List<Recording> otherSortedChildren = new ArrayList<>(otherList.elements);
			Collections.sort(otherSortedChildren);
			int sortedChildrenComparison = compareElements(mySortedChildren, otherSortedChildren);
			if (sortedChildrenComparison != 0) {
				return sortedChildrenComparison;
			}
			return compareElements(this.elements, otherList.elements);
		}

		return 0;
	}

	@Override
	public boolean isomorphicTo(Recording other) {
		if (other instanceof ListRecording otherList) {
			if (elements.isEmpty() || otherList.elements.isEmpty()) {
				return true;
			}
			return elements.getFirst().isomorphicTo(otherList.elements.getFirst());
		}
		return false;
	}

	private int compareElements(List<Recording> left, List<Recording> right) {
		int sizeComparison = Integer.compare(left.size(), right.size());
		if (sizeComparison != 0) {
			return sizeComparison;
		}
		for (int i = 0; i < left.size(); i++) {
			int childComparison = left.get(i).compareTo(right.get(i));
			if (childComparison != 0) {
				return childComparison;
			}
		}
		return 0;
	}

	@Override
	public String serialize() {
		return Serialization.serializeList(elements);
	}

	@Override
	public String toString() {
		List<String> elementsString = elements.stream().map(Object::toString).toList();
		return "list{%s}".formatted(String.join(",", elementsString));
	}

}
