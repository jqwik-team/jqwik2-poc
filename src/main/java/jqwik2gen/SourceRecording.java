package jqwik2gen;

import java.util.*;
import java.util.stream.*;

public sealed interface SourceRecording
	permits AtomicRecording, TreeRecording, UnshrinkableRecording {

	Iterator<Integer> iterator();

	List<SourceRecording> children();

	Stream<SourceRecording> shrink();
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

	@Override
	public Stream<SourceRecording> shrink() {
		// TODO: Implement shrinking
		return Stream.empty();
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
	public Stream<SourceRecording> shrink() {
		// TODO: Implement shrinking
		return Stream.empty();
	}

}

