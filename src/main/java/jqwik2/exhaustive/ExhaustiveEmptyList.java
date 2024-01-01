package jqwik2.exhaustive;

import jqwik2.*;
import jqwik2.api.*;

public class ExhaustiveEmptyList extends ExhaustiveList {

	private Exhaustive<?> prev;

	public ExhaustiveEmptyList() {
		// TODO: This is a hack to make ExhaustiveList work with empty lists
		super(0, null);
	}

	public int size() {
		return 0;
	}

	@Override
	public long maxCount() {
		return 1;
	}

	@Override
	public void advance() {
		if (prev == null) {
			Generator.noMoreValues();
		}
		prev.advance();
	}

	@Override
	public ExhaustiveSource clone() {
		return this;
	}

	@Override
	public void next() {
		advance();
	}

	@Override
	public void setPrev(Exhaustive<?> exhaustive) {
		prev = exhaustive;
	}

	@Override
	public void setSucc(Exhaustive<?> exhaustive) {
		// ignore
	}

	@Override
	public Atom atom() {
		throw new CannotGenerateException("Source is not an atom");
	}

	@Override
	public List list() {
		return this;
	}

	@Override
	public Tree tree() {
		throw new CannotGenerateException("Source is not a list");
	}

	@Override
	public GenSource nextElement() {
		throw new CannotGenerateException("No more elements");
	}

	@Override
	public String toString() {
		return "ExhaustiveEmptyList";
	}
}
