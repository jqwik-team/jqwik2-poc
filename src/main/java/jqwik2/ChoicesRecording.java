package jqwik2;

import java.util.*;
import java.util.stream.*;

public sealed interface ChoicesRecording extends Comparable<ChoicesRecording>
	permits AtomRecording, ListRecording, TreeRecording {

	Stream<? extends ChoicesRecording> shrink();
}

record AtomRecording(List<Integer> choices) implements ChoicesRecording {
	AtomRecording(Integer... choices) {
		this(new ArrayList<>(Arrays.asList(choices)));
	}

	@Override
	public Stream<? extends ChoicesRecording> shrink() {
		return new AtomShrinker(this).shrink();
	}

	@Override
	public int compareTo(ChoicesRecording other) {
		if (other instanceof AtomRecording otherAtomic) {
			return compareAtoms(this, otherAtomic);
		}
		return 0;
	}

	private int compareAtoms(AtomRecording left, AtomRecording right) {
		int sizeComparison = Integer.compare(left.choices().size(), right.choices().size());
		if (sizeComparison != 0) {
			return sizeComparison;
		}
		for (int i = 0; i < left.choices().size(); i++) {
			int seedComparison = Integer.compare(left.choices().get(i), right.choices().get(i));
			if (seedComparison != 0) {
				return seedComparison;
			}
		}
		return 0;
	}

	@Override
	public String toString() {
		List<String> listOfStrings = choices.stream().map(Object::toString).toList();
		return "atom{%s}".formatted(String.join(", ", listOfStrings));
	}
}

record ListRecording(List<ChoicesRecording> elements) implements ChoicesRecording {

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

// TODO: Tuples could have more strict shrinking, since elements cannot be exchanged and size is stable
// record TupleSource(List<SourceRecording> tuple) implements SourceRecording {
// 	@Override
// 	public Collection<SourceRecording> children() {
// 		return Collections.emptyList();
// 	}
// }

record TreeRecording(ChoicesRecording head, ChoicesRecording child) implements ChoicesRecording {

	@Override
	public Stream<? extends ChoicesRecording> shrink() {
		return new TreeShrinker(this).shrink();
	}

	@Override
	public int compareTo(ChoicesRecording other) {
		if (other instanceof TreeRecording otherTree) {
			int headComparison = this.head.compareTo(otherTree.head);
			if (headComparison != 0) {
				return headComparison;
			}
			return this.child.compareTo(otherTree.child);
		}
		return 0;
	}

	@Override
	public String toString() {
		return "tree{%s, %s}".formatted(head, child);
	}

}

