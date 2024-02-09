package jqwik2.internal.growing;

import jqwik2.api.*;
import jqwik2.internal.*;

abstract class AbstractGrowingSource<T extends GrowingSource<T>> implements GrowingSource<T>, GenSource {

	@Override
	public Choice choice() {
		throw new CannotGenerateException("This source is not a choice");
	}

	@Override
	public List list() {
		throw new CannotGenerateException("This source is not a list");
	}

	@Override
	public Tuple tuple() {
		throw new CannotGenerateException("This source is not a tuple");
	}

}
