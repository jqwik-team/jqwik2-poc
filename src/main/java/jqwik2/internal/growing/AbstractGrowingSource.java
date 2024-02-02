package jqwik2.internal.growing;

import java.util.*;

import jqwik2.api.*;
import jqwik2.internal.*;

abstract class AbstractGrowingSource implements GrowingSource, GenSource {

	@Override
	public Atom atom() {
		throw new CannotGenerateException("This source is not an atom");
	}

	@Override
	public List list() {
		throw new CannotGenerateException("This source is not an atom");
	}

	@Override
	public Tuple tuple() {
		throw new CannotGenerateException("This source is not an atom");
	}

}
