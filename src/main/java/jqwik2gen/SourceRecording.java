package jqwik2gen;

import java.util.*;

sealed interface SourceRecording
	permits AtomicRecording, TreeRecording, UnshrinkableRecording {

	Iterator<Integer> iterator();

	List<SourceRecording> children();
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
}

record AtomicRecording(int... seeds) implements SourceRecording {
	@Override
	public Iterator<Integer> iterator() {
		return Arrays.stream(seeds).iterator();
	}

	@Override
	public List<SourceRecording> children() {
		return Collections.emptyList();
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
}

