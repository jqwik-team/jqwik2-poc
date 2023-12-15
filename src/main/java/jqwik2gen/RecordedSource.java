package jqwik2gen;

import java.util.*;

sealed interface RecordedSource
	permits AtomicSource, TreeSource, UnshrinkableSource {

	Iterator<Integer> iterator();

	List<RecordedSource> children();
}

record UnshrinkableSource() implements RecordedSource {
	@Override
	public Iterator<Integer> iterator() {
		return Collections.emptyIterator();
	}

	@Override
	public List<RecordedSource> children() {
		return Collections.emptyList();
	}
}

record AtomicSource(int... seeds) implements RecordedSource {
	@Override
	public Iterator<Integer> iterator() {
		return Arrays.stream(seeds).iterator();
	}

	@Override
	public List<RecordedSource> children() {
		return Collections.emptyList();
	}
}

// record TupleSource(List<RecordedSource> tuple) implements RecordedSource {
// 	@Override
// 	public Collection<RecordedSource> children() {
// 		return Collections.emptyList();
// 	}
// }

record TreeSource(RecordedSource head, List<RecordedSource> children) implements RecordedSource {
	@Override
	public Iterator<Integer> iterator() {
		return head.iterator();
	}
}

