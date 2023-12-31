package jqwik2;

import jqwik2.api.*;

public class ExhaustiveTree implements GenSource.Tree, ExhaustiveSource {
	@Override
	public long maxCount() {
		return 0;
	}

	@Override
	public void advance() {

	}

	@Override
	public ExhaustiveSource clone() {
		return null;
	}

	@Override
	public void next() {

	}

	@Override
	public void setPrev(Exhaustive<?> exhaustive) {

	}

	@Override
	public void setSucc(Exhaustive<?> exhaustive) {

	}

	@Override
	public Atom atom() {
		throw new CannotGenerateException("Source is not an atom");
	}

	@Override
	public List list() {
		throw new CannotGenerateException("Source is not a list");
	}

	@Override
	public Tree tree() {
		return this;
	}

	@Override
	public GenSource head() {
		return null;
	}

	@Override
	public GenSource child() {
		return null;
	}
}
