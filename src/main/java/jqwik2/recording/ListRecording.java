package jqwik2.recording;

import java.util.*;
import java.util.stream.*;

public record ListRecording(List<ChoicesRecording> elements) implements ChoicesRecording {

	@Override
	public Stream<? extends ChoicesRecording> shrink() {
		return new ListShrinker(this).shrink();
	}

	@Override
	public int compareTo(ChoicesRecording other) {
		if (other instanceof ListRecording otherList) {
			List<ChoicesRecording> mySortedChildren = new ArrayList<>(this.elements);
			Collections.sort(mySortedChildren);
			List<ChoicesRecording> otherSortedChildren = new ArrayList<>(otherList.elements);
			Collections.sort(otherSortedChildren);
			int sortedChildrenComparison = compareElements(mySortedChildren, otherSortedChildren);
			if (sortedChildrenComparison != 0) {
				return sortedChildrenComparison;
			}
			return compareElements(this.elements, otherList.elements);
		}

		return 0;
	}

	private int compareElements(List<ChoicesRecording> left, List<ChoicesRecording> right) {
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
	public String toString() {
		List<String> elementsString = elements.stream().map(Object::toString).toList();
		return "list{%s}".formatted(String.join(",", elementsString));
	}

}
