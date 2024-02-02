package jqwik2.internal.growing;

import java.util.*;

import jqwik2.api.*;

public class GrowingSampleSource implements SampleSource {
	private final List<GrowingGenSource> sources = new ArrayList<>();

	@Override
	public List<GenSource> sources(int size) {
		while (sources.size() < size) {
			sources.add(new GrowingGenSource());
		}
		return sources.stream().map(s -> (GenSource) s).toList();
	}

	boolean advance() {
		for (int i = sources.size() - 1; i >= 0; i--) {
			var source = sources.get(i);
			if (source.advance()) {
				return true;
			} else {
				source.reset();
			}
		}
		return false;
	}
}
