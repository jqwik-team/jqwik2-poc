package jqwik2.internal.growing;

import jqwik2.api.*;

class GrowingList extends AbstractGrowingCollection implements GenSource.List {

	@Override
	public List list() {
		return this;
	}

	@Override
	public GenSource nextElement() {
		return nextSource();
	}

}
