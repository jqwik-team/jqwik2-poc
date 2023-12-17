package jqwik2gen;

import java.util.*;
import java.util.stream.*;

public sealed interface SourceRecording extends Comparable<SourceRecording>
	permits AtomicRecording, TreeRecording, UnshrinkableRecording {

	Iterator<Integer> iterator();

	List<SourceRecording> children();

	Stream<? extends SourceRecording> shrink();

	static int compareAtomic(AtomicRecording left, AtomicRecording right) {
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

}

record UnshrinkableRecording() implements SourceRecording {
	@Override
	public Iterator<Integer> iterator() {
		return Collections.emptyIterator();
	}

	@Override
	public List<SourceRecording> children() {
		return Collections.emptyList();
	}

	@Override
	public Stream<SourceRecording> shrink() {
		return Stream.empty();
	}

	@Override
	public int compareTo(SourceRecording other) {
		// All shrinkable recordings are larger than unshrinkable ones
		if (other instanceof UnshrinkableRecording) {
			return 0;
		}
		return -1;
	}
}

record AtomicRecording(List<Integer> seeds) implements SourceRecording {
	AtomicRecording(Integer... seeds) {
		this(Arrays.asList(seeds));
	}

	AtomicRecording() {
		this(new ArrayList<>());
	}

	int push(int seed) {
		seeds.add(seed);
		return seed;
	}

	int get(int index) {
		return seeds.get(index);
	}

	@Override
	public Iterator<Integer> iterator() {
		return seeds.iterator();
	}

	@Override
	public List<SourceRecording> children() {
		return Collections.emptyList();
	}

	@Override
	public Stream<? extends SourceRecording> shrink() {
		return new AtomicShrinker(this).shrink();
	}

	@Override
	public int compareTo(SourceRecording other) {
		if (other instanceof UnshrinkableRecording) {
			return 1;
		}
		if (other instanceof AtomicRecording otherAtomic) {
			return SourceRecording.compareAtomic(this, otherAtomic);
		}
		return 0;
	}

	@Override
	public String toString() {
		return "atomic{%s}".formatted(seeds);
	}
}

// record TupleSource(List<SourceRecording> tuple) implements SourceRecording {
// 	@Override
// 	public Collection<SourceRecording> children() {
// 		return Collections.emptyList();
// 	}
// }

record TreeRecording(SourceRecording head, List<SourceRecording> children) implements SourceRecording {

	@Override
	public Iterator<Integer> iterator() {
		return head.iterator();
	}

	@Override
	public Stream<? extends SourceRecording> shrink() {
		return new TreeShrinker(this).shrink();
	}

	public SourceRecording pushChild(SourceRecording recording) {
		children.add(recording);
		return recording;
	}

	@Override
	public int compareTo(SourceRecording other) {
		if (other instanceof UnshrinkableRecording) {
			return 1;
		}
		if (other instanceof TreeRecording otherTree) {
			int headComparison = this.head.compareTo(otherTree.head);
			if (headComparison != 0) {
				return headComparison;
			}
			List<SourceRecording> mySortedChildren = new ArrayList<>(this.children);
			Collections.sort(mySortedChildren);
			List<SourceRecording> otherSortedChildren = new ArrayList<>(otherTree.children);
			Collections.sort(otherSortedChildren);
			int sortedChildrenComparison = compareChildren(mySortedChildren, otherSortedChildren);
			if (sortedChildrenComparison != 0) {
				return sortedChildrenComparison;
			}
			return compareChildren(this.children, otherTree.children);
		}

		return 0;
	}

	private int compareChildren(List<SourceRecording> left, List<SourceRecording> right) {
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
		return "tree{%s, %s}".formatted(head, children);
	}

}

