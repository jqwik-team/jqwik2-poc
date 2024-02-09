package jqwik2.internal.growing;

import java.util.*;

import jqwik2.api.*;
import jqwik2.internal.*;

public class GrowingSampleSource extends SequentialGuidedGeneration implements SampleSource {

	private List<GrowingTuple> currentSizeSources = new ArrayList<>();
	private int currentIndex = 0;

	public GrowingSampleSource() {
		currentSizeSources.add(new GrowingTuple());
	}

	@Override
	public List<GenSource> sources(int size) {
		GrowingTuple current = currentSizeSources.get(currentIndex);
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
		if (currentIndex < currentSizeSources.size() - 1) {
			currentIndex++;
			return true;
		}
		List<GrowingTuple> nextSizeSources = new ArrayList<>();
		for (GrowingTuple source : currentSizeSources) {
			Set<GrowingTuple> grownSources = source.grow();
			nextSizeSources.addAll(grownSources);
		}
		currentSizeSources = nextSizeSources;
		currentIndex = 0;
		return currentSizeSources.size() > 0;
	}
}
