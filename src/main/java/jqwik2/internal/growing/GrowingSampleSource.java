package jqwik2.internal.growing;

import java.util.*;

import jqwik2.api.*;
import jqwik2.internal.*;

public class GrowingSampleSource extends SequentialGuidedGeneration implements SampleSource {
	private final List<GrowingGenSource> sources = new ArrayList<>();

	@Override
	public List<GenSource> sources(int size) {
		while (sources.size() < size) {
			sources.add(new GrowingGenSource());
		}
		return sources.stream().map(s -> (GenSource) s).toList();
	}

	@Override
	protected SampleSource initialSource() {
		return this;
	}

	@Override
	protected SampleSource nextSource() {
		return this;
	}

	@Override
	protected boolean handleResult(TryExecutionResult result, Sample sample) {
		return grow();
	}

	@Override
	protected boolean handleEmptyGeneration(SampleSource failingSource) {
		return grow();
	}

	private boolean grow() {
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
