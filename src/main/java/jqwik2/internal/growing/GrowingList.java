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
		java.util.List<GrowingSourceContainer> newSources = new ArrayList<>(sources);
		newSources.set(position, container);
		return new GrowingList(newSources);
	}

}
