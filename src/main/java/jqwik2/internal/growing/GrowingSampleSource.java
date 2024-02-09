package jqwik2.internal.growing;

import java.util.*;

import jqwik2.api.*;
import jqwik2.internal.*;

public class GrowingSampleSource extends SequentialGuidedGeneration implements SampleSource {

	private Set<GrowingTuple> currentSizeSources = new LinkedHashSet<>();
	private Iterator<GrowingTuple> iterator;

	public GrowingSampleSource() {
		currentSizeSources.add(new GrowingTuple());
		iterator = currentSizeSources.iterator();
	}

	@Override
	public List<GenSource> sources(int size) {
		GrowingTuple current = iterator.next();
		// GrowingTuple current = currentSizeSources.get(currentIndex);
		List<GenSource> sources = new ArrayList<>();
		while (sources.size() < size) {
			sources.add(current.nextValue());
		}
		return sources;
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
		if (iterator.hasNext()) {
			return true;
		}
		Set<GrowingTuple> nextSizeSources = new LinkedHashSet<>();
		for (GrowingTuple source : currentSizeSources) {
			Set<GrowingTuple> grownSources = source.grow();
			nextSizeSources.addAll(grownSources);
		}
		currentSizeSources = nextSizeSources;
		iterator = currentSizeSources.iterator();
		return !currentSizeSources.isEmpty();
	}
}
