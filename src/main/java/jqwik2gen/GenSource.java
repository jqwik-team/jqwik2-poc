package jqwik2gen;

import java.util.*;

public sealed interface GenSource permits RandomGenSource, ReproducingSource {
	int next(int max);

	GenSource child();
}

final class ReproducingSource implements GenSource {
	private final Iterator<Integer> iterator;
	private final Iterator<RecordedSource> children;

	public ReproducingSource(RecordedSource source) {
		this.children = source.children().iterator();
		this.iterator = source.iterator();
	}

	@Override
	public int next(int max) {
		return iterator.next();
	}

	@Override
	public GenSource child() {
		if (children.hasNext())
			return new ReproducingSource(children.next());
		else
			throw new IllegalStateException("No more children!");
	}
}