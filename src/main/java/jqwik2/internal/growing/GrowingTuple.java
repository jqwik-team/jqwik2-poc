package jqwik2.internal.growing;

import java.util.*;

import jqwik2.api.*;

class GrowingTuple extends AbstractGrowingCollection<GrowingTuple> implements GenSource.Tuple {

	GrowingTuple() {
		super();
	}

	GrowingTuple(java.util.List<GrowingSourceContainer> sources) {
		super(sources);
	}

	@Override
	public Tuple tuple() {
		return this;
	}

	@Override
	public GenSource nextValue() {
		return nextSource();
	}

	@Override
	protected GrowingTuple replace(int position, GrowingSourceContainer container) {
		java.util.List<GrowingSourceContainer> clonedSources =
			sources.stream().map(GrowingSourceContainer::copy).toList();
		java.util.List<GrowingSourceContainer> newSources = new ArrayList<>(clonedSources);
		newSources.set(position, container);
		return new GrowingTuple(newSources);
	}

	@Override
	public GrowingTuple copy() {
		java.util.List<GrowingSourceContainer> clonedSources =
			sources.stream().map(GrowingSourceContainer::copy).toList();
		return new GrowingTuple(clonedSources);
	}
}
