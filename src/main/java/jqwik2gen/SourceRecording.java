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
	public Stream<SourceRecording> shrink() {
		// TODO: Shrink each seed from existing to 0 with in-between steps
		List<SourceRecording> shrinkings = new ArrayList<>();
		for (int i = seeds.size() - 1; i >= 0; i--) {
			List<Integer> shrunk = new ArrayList<>(seeds);
			if (shrunk.get(i) > 0) {
				shrunk.set(i, 0);
				shrinkings.add(new AtomicRecording(shrunk));
			}
		}
		return shrinkings.stream();
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

