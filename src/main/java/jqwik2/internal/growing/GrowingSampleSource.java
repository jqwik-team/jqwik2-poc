package jqwik2.internal.growing;

import java.util.*;

import jqwik2.api.*;

class GrowingSampleSource implements SampleSource {
	private final List<GrowingGenSource> sources = new ArrayList<>();

	@Override
	public List<GenSource> sources(int size) {
		while (sources.size() < size) {
			sources.add(new GrowingGenSource());
		}
		return sources.stream().map(s -> (GenSource) s).toList();
	}

	boolean next() {
		sources.forEach(GrowingGenSource::next);
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

	private void resetSourcesAfter(int resourceIndex) {
		sources.subList(resourceIndex + 1, sources.size()).forEach(GrowingGenSource::reset);
	}
}
