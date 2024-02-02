package jqwik2.internal.growing;

import java.util.*;

import jqwik2.api.*;

class AbstractGrowingCollection extends AbstractGrowingSource {

	private final java.util.List<GrowingSourceContainer> sources = new ArrayList<>();
	private int currentSourceIndex = 0;

	protected GenSource nextSource() {
		if (currentSourceIndex >= sources.size()) {
			var container = new GrowingSourceContainer();
			sources.add(container);
		}
		return sources.get(currentSourceIndex++).get(GenSource.class, GrowingGenSource::new);
	}

	@Override
	public boolean advance() {
		int i = sources.size() - 1;
		while (i >= 0) {
			var source = sources.get(i);
			if (source.advance()) {
				resetSourcesAfter(i);
				return true;
			}
			i--;
		}
		return false;
	}

	private void resetSourcesAfter(int index) {
		sources.subList(index + 1, sources.size()).forEach(GrowingSourceContainer::reset);
	}

	@Override
	public void reset() {
		sources.forEach(GrowingSourceContainer::reset);
	}

	@Override
	public void next() {
		currentSourceIndex = 0;
		sources.forEach(GrowingSourceContainer::next);
	}
}
