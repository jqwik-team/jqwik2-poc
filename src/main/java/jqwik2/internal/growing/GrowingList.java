package jqwik2.internal.growing;

import java.util.*;

import jqwik2.api.*;

class GrowingList extends AbstractGrowingCollection<GrowingList> implements GenSource.List {

	GrowingList() {
		super();
	}

	GrowingList(java.util.List<GrowingSourceContainer> newSources) {
		super(newSources);
	}

	@Override
	public List list() {
		return this;
	}

	@Override
	public GenSource nextElement() {
		return nextSource();
	}

	@Override
	protected GrowingList replace(int position, GrowingSourceContainer container) {
		var clonedSources = sources.stream().map(GrowingSourceContainer::copy).toList();
		java.util.List<GrowingSourceContainer> newSources = new ArrayList<>(clonedSources);
		newSources.set(position, container);
		return new GrowingList(newSources);
	}

	@Override
	public GrowingList copy() {
		var clonedSources = sources.stream().map(GrowingSourceContainer::copy).toList();
		return new GrowingList(clonedSources);
	}
}
