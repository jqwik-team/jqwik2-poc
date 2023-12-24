package jqwik2.api;

import java.util.*;
import java.util.stream.*;

import jqwik2.*;

public record Sample(List<Shrinkable<Object>> shrinkables) implements Comparable<Sample> {

	public List<Object> values() {
		return shrinkables().stream()
							.map(Shrinkable::value)
							.toList();
	}

	public List<Object> regenerateValues() {
		return shrinkables().stream().map(Shrinkable::regenerate).toList();
	}

	public Stream<Sample> shrink() {
		return new SampleShrinker(this).shrink();
	}

	@Override
	public int compareTo(Sample other) {
		List<Shrinkable<Object>> mySortedShrinkables = new ArrayList<>(shrinkables);
		Collections.sort(mySortedShrinkables);
		List<Shrinkable<Object>> otherSortedShrinkables = new ArrayList<>(other.shrinkables);
		Collections.sort(otherSortedShrinkables);
		int sortedChildrenComparison = compareShrinkables(mySortedShrinkables, otherSortedShrinkables);
		if (sortedChildrenComparison != 0) {
			return sortedChildrenComparison;
		}
		return compareShrinkables(shrinkables, other.shrinkables);
	}

	private int compareShrinkables(List<Shrinkable<Object>> left, List<Shrinkable<Object>> right) {
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
		return "Sample{%s}".formatted(values().stream().map(Object::toString).toList());
	}

	public int size() {
		return shrinkables().size();
	}
}
