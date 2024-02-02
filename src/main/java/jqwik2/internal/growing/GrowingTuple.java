package jqwik2.internal.growing;

import java.util.*;

import jqwik2.api.*;
import jqwik2.internal.*;

class GrowingTuple extends AbstractGrowingSource implements GenSource.Tuple {

	private final java.util.List<GrowingSourceContainer> values = new ArrayList<>();
	private int currentValueIndex = 0;

	@Override
	public Tuple tuple() {
		return this;
	}

	@Override
	public GenSource nextValue() {
		if (currentValueIndex >= values.size()) {
			var container = new GrowingSourceContainer();
			values.add(container);
		}
		return values.get(currentValueIndex++).get(GenSource.class, GrowingGenSource::new);
	}

	@Override
	public boolean advance() {
		int i = values.size() - 1;
		while (i >= 0) {
			var source = values.get(i);
			if (source.advance()) {
				resetValuesAfter(i);
				return true;
			}
			i--;
		}
		return false;
	}

	private void resetValuesAfter(int index) {
		values.subList(index + 1, values.size()).forEach(GrowingSourceContainer::reset);
	}

	@Override
	public void reset() {
		values.forEach(GrowingSourceContainer::reset);
	}

	@Override
	public void next() {
		currentValueIndex = 0;
		values.forEach(GrowingSourceContainer::next);
	}
}
