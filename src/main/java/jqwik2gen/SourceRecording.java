package jqwik2gen;

import java.util.*;
import java.util.stream.*;

public sealed interface SourceRecording extends Comparable<SourceRecording>
	permits AtomRecording, ListRecording, TreeRecording, UnshrinkableRecording {

	Iterator<Integer> iterator();

	Stream<? extends SourceRecording> shrink();

	SourceRecording UNSHRINKABLE = new UnshrinkableRecording();

}

record UnshrinkableRecording() implements SourceRecording {
	@Override
	public Iterator<Integer> iterator() {
		return Collections.emptyIterator();
	}

	@Override
	public Stream<SourceRecording> shrink() {
		return Stream.empty();
	}

	@Override
	public int compareTo(SourceRecording other) {
		return 0;
	}
}

record AtomRecording(List<Integer> seeds) implements SourceRecording {
	AtomRecording(Integer... seeds) {
		this(Arrays.asList(seeds));
	}

	@Override
	public Iterator<Integer> iterator() {
		return seeds.iterator();
	}

	@Override
	public Stream<? extends SourceRecording> shrink() {
		return new AtomicShrinker(this).shrink();
	}

	@Override
	public int compareTo(SourceRecording other) {
		if (other instanceof AtomRecording otherAtomic) {
			return compareAtoms(this, otherAtomic);
		}
		return 0;
	}

	private int compareAtoms(AtomRecording left, AtomRecording right) {
		int sizeComparison = Integer.compare(left.seeds().size(), right.seeds().size());
		if (sizeComparison != 0) {
			return sizeComparison;
		}
		for (int i = 0; i < left.seeds().size(); i++) {
			int seedComparison = Integer.compare(left.seeds().get(i), right.seeds().get(i));
			if (seedComparison != 0) {
				return seedComparison;
			}
		}
		return 0;
	}

	@Override
	public String toString() {
		return "atomic{%s}".formatted(seeds);
	}
}

record ListRecording(List<SourceRecording> elements) implements SourceRecording {

	@Override
	public Iterator<Integer> iterator() {
		return Collections.emptyIterator();
	}

	@Override
	public Stream<? extends SourceRecording> shrink() {
		return new ListShrinker(this).shrink();
	}

	@Override
	public int compareTo(SourceRecording other) {
		if (other instanceof ListRecording otherList) {
			List<SourceRecording> mySortedChildren = new ArrayList<>(this.elements);
			Collections.sort(mySortedChildren);
			List<SourceRecording> otherSortedChildren = new ArrayList<>(otherList.elements);
			Collections.sort(otherSortedChildren);
			int sortedChildrenComparison = compareElements(mySortedChildren, otherSortedChildren);
			if (sortedChildrenComparison != 0) {
				return sortedChildrenComparison;
			}
			return compareElements(this.elements, otherList.elements);
		}

		return 0;
	}

	private int compareElements(List<SourceRecording> left, List<SourceRecording> right) {
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

record TreeRecording(SourceRecording head, SourceRecording child) implements SourceRecording {

	@Override
	public Iterator<Integer> iterator() {
		return head.iterator();
	}

	@Override
	public Stream<? extends SourceRecording> shrink() {
		return new TreeShrinker(this).shrink();
	}

	@Override
	public int compareTo(SourceRecording other) {
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

