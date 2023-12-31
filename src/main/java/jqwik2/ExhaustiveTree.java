package jqwik2;

import java.util.function.*;

import jqwik2.api.*;

import static jqwik2.ExhaustiveSource.*;

public class ExhaustiveTree implements GenSource.Tree, ExhaustiveSource {
	private final Function<Integer, ExhaustiveSource> childCreator;
	private final java.util.List<Integer> heads;
	private final java.util.List<ExhaustiveSource> children;
	private int current = 0;

	public ExhaustiveTree(java.util.List<Integer> heads, Function<Integer, ExhaustiveSource> childCreator) {
		this.heads = heads;
		this.childCreator = childCreator;
		this.children = createChildren();
	}

	private java.util.List<ExhaustiveSource> createChildren() {
		java.util.List<ExhaustiveSource> result = new java.util.ArrayList<>();
		for (int head : heads) {
			result.add(childCreator.apply(head));
		}
		return result;
	}

	@Override
	public long maxCount() {
		return children.stream().mapToLong(ExhaustiveSource::maxCount).sum();
	}

	@Override
	public void advance() {

	}

	@Override
	public ExhaustiveTree clone() {
		return new ExhaustiveTree(heads, childCreator);
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
		return ExhaustiveSource.atom(
			value(heads.get(current))
		);
	}

	@Override
	public GenSource child() {
		return children.get(current);
	}
}
