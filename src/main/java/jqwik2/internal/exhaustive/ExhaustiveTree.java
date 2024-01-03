package jqwik2.internal.exhaustive;

import java.util.*;
import java.util.function.*;
import java.util.stream.*;

import jqwik2.api.*;
import jqwik2.api.recording.*;

public class ExhaustiveTree extends AbstractExhaustiveSource<GenSource.Tree> {
	private final Function<GenSource, ExhaustiveSource<?>> childCreator;
	private final ExhaustiveSource<?> head;
	private ExhaustiveSource<?> child;

	public ExhaustiveTree(ExhaustiveChoice.Range range, Function<Integer, ExhaustiveSource<?>> childCreator) {
		this(new ExhaustiveAtom(range), childCreatorFromSource(childCreator));
	}

	private static Function<GenSource, ExhaustiveSource<?>> childCreatorFromSource(Function<Integer, ExhaustiveSource<?>> childCreator) {
		return source -> {
			int size = source.atom().choose(Integer.MAX_VALUE);
			return childCreator.apply(size);
		};
	}

	public ExhaustiveTree(ExhaustiveSource<?> head, Function<GenSource, ExhaustiveSource<?>> childCreator) {
		this.childCreator = childCreator;
		this.head = head;
		creatAndChainChild();
	}

	private void creatAndChainChild() {
		child = childCreator.apply(head.get());
		head.chain(child);
		succ().ifPresent(succ -> child.setSucc(succ));
	}

	private static List<Integer> createHeads(ExhaustiveChoice.Range range) {
		return IntStream.range(range.min(), range.max() + 1)
						.boxed()
						.collect(Collectors.toList());
	}

	private List<ExhaustiveSource<?>> createChildren() {
		ExhaustiveSource<?> iterator = head.clone();
		List<ExhaustiveSource<?>> result = new ArrayList<>();
		while (true) {
			result.add(childCreator.apply(iterator.get()));
			try {
				iterator.next();
			} catch (Generator.NoMoreValues e) {
				break;
			}
		}
		return result;
	}

	@Override
	public long maxCount() {
		// TODO: Optimize to not create all children
		return createChildren().stream().mapToLong(ExhaustiveSource::maxCount).sum();
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

	@Override
	public ExhaustiveTree clone() {
		return new ExhaustiveTree(head.clone(), childCreator);
	}

	@Override
	public void setSucc(Exhaustive<?> exhaustive) {
		super.setSucc(exhaustive);
		child.setSucc(exhaustive);
	}

	@Override
	public Recording recording() {
		return Recording.tree(head.recording(), child.recording());
	}
}
