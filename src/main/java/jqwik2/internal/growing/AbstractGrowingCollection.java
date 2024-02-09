package jqwik2.internal.growing;

import java.util.*;
import java.util.stream.*;

import jqwik2.api.*;

abstract class AbstractGrowingCollection<T extends GrowingSource<T>> extends AbstractGrowingSource<T> {

	protected final java.util.List<GrowingSourceContainer> sources;
	private int currentSourceIndex = 0;

	protected GenSource nextSource() {
		if (currentSourceIndex >= sources.size()) {
			var container = new GrowingSourceContainer();
			sources.add(container);
		}
		return sources.get(currentSourceIndex++).get(GenSource.class, GrowingGenSource::new);
	}

	AbstractGrowingCollection() {
		this(new ArrayList<>());
	}

	AbstractGrowingCollection(java.util.List<GrowingSourceContainer> sources) {
		this.sources = sources;
	}

	@Override
	public Set<T> grow() {
		Set<T> result = new HashSet<>();
		for (int i = 0; i < sources.size(); i++) {
			int index = i;
			GrowingSourceContainer source = sources.get(i);
			Set<GrowingSourceContainer> grown = source.grow();
			result.addAll(grown.stream().map(c -> replace(index, c)).collect(Collectors.toSet()));
		}
		return result;
	}

	protected abstract T replace(int position, GrowingSourceContainer source);
}
