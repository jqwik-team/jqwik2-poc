package jqwik2.internal.exhaustive;

import java.util.*;
import java.util.function.*;

import jqwik2.api.*;
import jqwik2.api.recording.*;

public class ExhaustiveTree extends AbstractExhaustiveSource<GenSource.Tree> {
	private final Function<GenSource, Optional<ExhaustiveSource<?>>> childCreator;
	private final ExhaustiveSource<?> head;
	private Optional<ExhaustiveSource<?>> optionalChild = Optional.empty();

	public ExhaustiveTree(ExhaustiveSource<?> head, Function<GenSource, Optional<ExhaustiveSource<?>>> childCreator) {
		this.childCreator = childCreator;
		this.head = head;
		creatAndChainChild();
	}

	private void creatAndChainChild() {
		optionalChild = childCreator.apply(head.current());
		optionalChild.ifPresent(child -> {
			head.chain(child);
			succ().ifPresent(child::setSucc);
		});
	}

	@Override
	public long maxCount() {
		long sum = 0;
		for (GenSource genSource : head) {
			Optional<ExhaustiveSource<?>> optionalChild = childCreator.apply(genSource);
			if (optionalChild.isPresent()) {
				sum += optionalChild.get().maxCount();
			} else {
				return Exhaustive.INFINITE;
			}
		}
		return sum;
	}

	@Override
	protected boolean tryAdvance() {
		if (!head.advanceThisOrUp()) {
			return false;
		}
		creatAndChainChild();
		return true;
	}

	@Override
	public boolean advance() {
		Recording before = head.recording();
		if (head.advance()) {
			Recording after = head.recording();
			if (!before.equals(after)) {
				creatAndChainChild();
			}
			return true;
		}
		if (prev().isEmpty()) {
			return false;
		}
		reset();
		return prev().get().advanceThisOrUp();
	}

	@Override
	public void reset() {
		head.reset();
		creatAndChainChild();
	}

	@Override
	public ExhaustiveTree clone() {
		return new ExhaustiveTree(head.clone(), childCreator);
	}

	@Override
	public void setSucc(Exhaustive<?> exhaustive) {
		super.setSucc(exhaustive);
		optionalChild.ifPresent(child -> child.setSucc(exhaustive));
	}

	@Override
	public Recording recording() {
		return Recording.tree(
			head.recording(),
			optionalChild.get().recording() // TODO: Handle empty optional
		);
	}
}
