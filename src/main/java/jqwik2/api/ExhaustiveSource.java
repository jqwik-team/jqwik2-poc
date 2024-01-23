package jqwik2.api;

import java.util.*;
import java.util.function.*;

import jqwik2.api.recording.*;
import jqwik2.internal.exhaustive.*;
import jqwik2.internal.recording.*;

public interface ExhaustiveSource<T extends GenSource> extends Exhaustive<ExhaustiveSource<T>>, Iterable<T> {

	static Optional<ExhaustiveSource<?>> any() {
		ExhaustiveSource<?> any = new AnyExhaustiveSource<>();
		return Optional.of(any);
	}

	static ExhaustiveChoice.Range range(int min, int max) {
		return new ExhaustiveChoice.Range(min, max);
	}

	static ExhaustiveChoice.Range value(int value) {
		return range(value, value);
	}

	static Optional<ExhaustiveSource<?>> atom(int... maxChoices) {
		ExhaustiveAtom exhaustiveAtom = new ExhaustiveAtom(maxChoices);
		if (exhaustiveAtom.maxCount() == Exhaustive.INFINITE) {
			return Optional.empty();
		}
		return Optional.of(exhaustiveAtom);
	}

	static Optional<ExhaustiveSource<?>> atom(ExhaustiveChoice.Range... ranges) {
		ExhaustiveAtom exhaustiveAtom = new ExhaustiveAtom(ranges);
		if (exhaustiveAtom.maxCount() == Exhaustive.INFINITE) {
			return Optional.empty();
		}
		return Optional.of(exhaustiveAtom);
	}

	static Optional<ExhaustiveSource<?>> or(Optional<ExhaustiveSource<?>>... alternatives) {
		if (Arrays.stream(alternatives).anyMatch(Optional::isEmpty)) {
			return Optional.empty();
		}
		List<? extends ExhaustiveSource<?>> atomList = Arrays.stream(alternatives).map(Optional::get).toList();
		ExhaustiveOr exhaustiveOr = new ExhaustiveOr(atomList);
		if (exhaustiveOr.maxCount() == Exhaustive.INFINITE) {
			return Optional.empty();
		}
		return Optional.of(exhaustiveOr);
	}

	static Optional<ExhaustiveSource<?>> list(int size, Optional<? extends ExhaustiveSource<?>> elementSource) {
		if (elementSource.isEmpty()) {
			return Optional.empty();
		}
		if (size == 0) {
			return Optional.of(new ExhaustiveEmptyCollection());
		}
		ExhaustiveList exhaustiveList = new ExhaustiveList(size, elementSource.get());
		if (exhaustiveList.maxCount() == Exhaustive.INFINITE) {
			return Optional.empty();
		}
		return Optional.of(exhaustiveList);
	}

	static Optional<ExhaustiveSource<?>> set(int size, Optional<? extends ExhaustiveSource<?>> elementSource) {
		if (elementSource.isEmpty()) {
			return Optional.empty();
		}
		if (size == 0) {
			return Optional.of(new ExhaustiveEmptyCollection());
		}
		ExhaustiveSet exhaustiveSet = new ExhaustiveSet(size, elementSource.get());
		if (exhaustiveSet.maxCount() == Exhaustive.INFINITE) {
			return Optional.empty();
		}
		return Optional.of(exhaustiveSet);
	}

	/**
	 * Generate tree from given head source and child creator function
	 *
	 * @param head         Source for head value
	 * @param childCreator Function to create child source based on head value
	 */
	static Optional<ExhaustiveSource<?>> tree(Optional<ExhaustiveSource<?>> head, Function<GenSource, Optional<ExhaustiveSource<?>>> childCreator) {
		if (head.isEmpty()) {
			return Optional.empty();
		}
		ExhaustiveTree exhaustiveTree = new ExhaustiveTree(head.get(), childCreator);
		if (exhaustiveTree.maxCount() == Exhaustive.INFINITE) {
			return Optional.empty();
		}
		return Optional.of(exhaustiveTree);
	}

	Recording recording();

	@SuppressWarnings("unchecked")
	default T current() {
		return (T) RecordedSource.of(recording());
	}

	class AnyExhaustiveSource<T extends GenSource> extends AbstractExhaustiveSource<T> {

		@Override
		public long maxCount() {
			return 1;
		}

		@Override
		public void reset() {

		}

		@Override
		public Recording recording() {
			return Recording.atom();
		}

		/**
		 * Try to advance this exhaustive source locally.
		 * Return true if successful, false if exhausted.
		 */
		@Override
		protected boolean tryAdvance() {
			return false;
		}

		@Override
		public ExhaustiveSource<T> clone() {
			// Necessary since prev and succ can be different per instance
			return new AnyExhaustiveSource<>();
		}
	}
}
