package jqwik2.exhaustive;

import java.util.function.*;
import java.util.stream.*;

import jqwik2.*;
import jqwik2.api.*;
import jqwik2.api.recording.*;

public class ExhaustiveTree extends AbstractExhaustiveSource<GenSource.Tree> implements GenSource.Tree {
	private final ExhaustiveChoice.Range range;
	private final Function<Integer, ExhaustiveSource<?>> childCreator;
	private final ExhaustiveAtom head;
	private ExhaustiveSource<?> child;

	public ExhaustiveTree(ExhaustiveChoice.Range range, Function<Integer, ExhaustiveSource<?>> childCreator) {
		this.range = range;
		this.childCreator = childCreator;
		this.head = new ExhaustiveAtom(range);
		creatAndChainChild();
	}

	private static java.util.List<Integer> createHeads(ExhaustiveChoice.Range range) {
		return IntStream.range(range.min(), range.max() + 1)
						.boxed()
						.collect(Collectors.toList());
	}

	private java.util.List<ExhaustiveSource<?>> createChildren(java.util.List<Integer> heads) {
		java.util.List<ExhaustiveSource<?>> result = new java.util.ArrayList<>();
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
	protected boolean tryAdvance() {
		try {
			head.advance();
			creatAndChainChild();
			return true;
		} catch (Generator.NoMoreValues e) {
			return false;
		}
	}

	@Override
	public void next() {
		Recording before = head.recording();
		try {
			head.next();
		} catch (Generator.NoMoreValues e) {
			if (prev().isPresent()) {
				reset();
				prev().get().advance();
			} else {
				Generator.noMoreValues();
			}
		}
		Recording after = head.recording();
		if (!before.equals(after)) {
			creatAndChainChild();
		}
	}

	@Override
	public void reset() {
		head.reset();
		creatAndChainChild();
	}

	private void creatAndChainChild() {
		int size = ((Atom) head.get()).choose(Integer.MAX_VALUE);
		child = childCreator.apply(size);
		head.chain(child);
		succ().ifPresent(succ -> child.setSucc(succ));
	}

	@Override
	public ExhaustiveTree clone() {
		return new ExhaustiveTree(range, childCreator);
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
		return Recording.tree(head.recording(), child.recording());
	}
}