package jqwik2;

import java.util.*;
import java.util.stream.*;

public record Sample(List<Shrinkable<Object>> shrinkables) implements Comparable<Sample> {

	public List<Object> values() {
		return shrinkables().stream()
							.map(Shrinkable::value)
							.toList();
	}

	List<Object> regenerateValues() {
		return shrinkables().stream().map(Shrinkable::regenerate).toList();
	}

	public Stream<Sample> shrink() {
		if (size() != 1) {
			throw new IllegalArgumentException("Only one parameter supported for now!");
		}
		Shrinkable<Object> firstParam = shrinkables().getFirst();
		return firstParam.shrink().map((Shrinkable<Object> s) -> new Sample(List.of(s)));
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

	int size() {
		return shrinkables().size();
	}
}
