package jqwik2gen;

import java.util.*;

public sealed interface GenSource permits RandomSource, ReproducingSource {
	int next(int max);
}

final class ReproducingSource implements GenSource {
	private final RecordedSource source;
	private final Iterator<Integer> iterator;

	public ReproducingSource(RecordedSource source) {
		this.source = source;
		this.iterator = source.stream().iterator();
	}

	@Override
	public int next(int max) {
		return iterator.next();
	}
}