package jqwik2;

import java.util.function.*;

import jqwik2.api.*;

public class ExhaustiveTree implements GenSource.Tree, ExhaustiveSource {
	private final ExhaustiveAtom head;
	private final Function<Integer[], ExhaustiveSource> childCreator;

	public ExhaustiveTree(ExhaustiveAtom head, Function<Integer[], ExhaustiveSource> childCreator) {
		this.head = head;
		this.childCreator = childCreator;
	}

	@Override
	public long maxCount() {
		return head.maxCount();
	}

	@Override
	public void advance() {

	}

	@Override
	public ExhaustiveTree clone() {
		return new ExhaustiveTree(head, childCreator);
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
