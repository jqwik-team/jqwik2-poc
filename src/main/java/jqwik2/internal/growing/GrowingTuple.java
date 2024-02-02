package jqwik2.internal.growing;

import jqwik2.api.*;

class GrowingTuple extends AbstractGrowingCollection implements GenSource.Tuple {

	@Override
	public Tuple tuple() {
		return this;
	}

	@Override
	public GenSource nextValue() {
		return nextSource();
	}
}
