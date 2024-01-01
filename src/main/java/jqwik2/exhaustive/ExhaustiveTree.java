package jqwik2.exhaustive;

import java.util.function.*;
import java.util.stream.*;

import jqwik2.*;
import jqwik2.api.*;
import jqwik2.api.recording.*;

public class ExhaustiveTree extends AbstractExhaustiveSource implements GenSource.Tree {
	private final ExhaustiveChoice.Range range;
	private final Function<Integer, ExhaustiveSource> childCreator;
	private final ExhaustiveAtom head;
	private ExhaustiveSource child;

	public ExhaustiveTree(ExhaustiveChoice.Range range, Function<Integer, ExhaustiveSource> childCreator) {
		this.range = range;
		this.childCreator = childCreator;
		this.head = new ExhaustiveAtom(range);
		this.head.setPrev(this);
		creatAndChainChild();
	}

	private static java.util.List<Integer> createHeads(ExhaustiveChoice.Range range) {
		return IntStream.range(range.min(), range.max() + 1)
						.boxed()
						.collect(Collectors.toList());
	}

	private java.util.List<ExhaustiveSource> createChildren(java.util.List<Integer> heads) {
		java.util.List<ExhaustiveSource> result = new java.util.ArrayList<>();
		for (int head : heads) {
			result.add(childCreator.apply(head));
		}
		return result;
	}

	@Override
	public long maxCount() {
		// TODO: Optimize to not create all children
		return createChildren(createHeads(range)).stream().mapToLong(ExhaustiveSource::maxCount).sum();
	}

	@Override
	public void advance() {
		head.advance();
		creatAndChainChild();
	}

	@Override
	public void reset() {
		head.reset();
		creatAndChainChild();
	}

	private void creatAndChainChild() {
		int size = ((Atom) head.fix()).choose(Integer.MAX_VALUE);
		child = childCreator.apply(size);
		head.chain(child);
		succ().ifPresent(succ -> child.setSucc(succ));
	}

	@Override
	public ExhaustiveTree clone() {
		return new ExhaustiveTree(range, childCreator);
	}

	@Override
	public void next() {
		head.next();
		creatAndChainChild();
	}

	@Override
	public void setPrev(Exhaustive<?> exhaustive) {
		super.setPrev(exhaustive);
	}

	@Override
	public void setSucc(Exhaustive<?> exhaustive) {
		super.setSucc(exhaustive);
		child.setSucc(exhaustive);
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
		return head;
	}

	@Override
	public GenSource child() {
		return child;
	}

	@Override
	public Recording recording() {
		throw new UnsupportedOperationException();
	}
}
