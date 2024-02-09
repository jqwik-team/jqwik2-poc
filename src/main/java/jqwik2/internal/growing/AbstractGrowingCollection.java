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
		this.sources = new ArrayList<>(sources);
	}

	@Override
	public Set<T> grow() {
		Set<T> result = new HashSet<>();
		for (int i = 0; i < sources.size(); i++) {
			int index = i;
			GrowingSourceContainer source = sources.get(i);
			Set<GrowingSourceContainer> grown = source.grow();
			Set<T> grownContainers = grown.stream()
										  .map(c -> replace(index, c))
										  .collect(Collectors.toSet());
			result.addAll(grownContainers);
		}
		return result;
	}

	protected abstract T replace(int position, GrowingSourceContainer source);

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;

		AbstractGrowingCollection<?> that = (AbstractGrowingCollection<?>) o;
		return Objects.equals(sources, that.sources);
	}

	@Override
	public int hashCode() {
		return sources != null ? sources.hashCode() : 0;
	}
}
