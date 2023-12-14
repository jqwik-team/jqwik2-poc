package jqwik2gen;

import java.util.*;
import java.util.stream.*;

sealed interface RecordedSource
	permits AtomicSource, TreeSource, TupleSource, UnshrinkableSource {

	Stream<Integer> stream();
}

record UnshrinkableSource() implements RecordedSource {
	@Override
	public Stream<Integer> stream() {
		return Stream.empty();
	}
}

record AtomicSource(int seed) implements RecordedSource {
	@Override
	public Stream<Integer> stream() {
		return Stream.of(seed);
	}
}

record TupleSource(List<RecordedSource> tuple) implements RecordedSource {
	@Override
	public Stream<Integer> stream() {
		return tuple.stream().flatMap(RecordedSource::stream);
	}
}

record TreeSource(RecordedSource head, List<TreeSource> children) implements RecordedSource {
	@Override
	public Stream<Integer> stream() {
		return Stream.concat(head.stream(), children.stream().flatMap(RecordedSource::stream));
	}
}

