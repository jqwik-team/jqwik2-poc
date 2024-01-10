package jqwik2.api;

import java.util.*;
import java.util.function.*;

import jqwik2.api.recording.*;
import jqwik2.internal.exhaustive.*;
import jqwik2.internal.recording.*;

public interface ExhaustiveSource<T extends GenSource> extends Exhaustive<ExhaustiveSource<T>>, Iterable<T> {

	static <T extends GenSource> Optional<ExhaustiveSource<T>> any() {
		ExhaustiveSource<T> any = new AnyExhaustiveSource<>();
		return Optional.of(any);
	}

	static ExhaustiveChoice.Range range(int min, int max) {
		return new ExhaustiveChoice.Range(min, max);
	}

	static ExhaustiveChoice.Range value(int value) {
		return range(value, value);
	}

	static Optional<ExhaustiveAtom> atom(int... maxChoices) {
		ExhaustiveAtom exhaustiveAtom = new ExhaustiveAtom(maxChoices);
		if (exhaustiveAtom.maxCount() == Exhaustive.INFINITE) {
			return Optional.empty();
		}
		return Optional.of(exhaustiveAtom);
	}

	static Optional<ExhaustiveAtom> atom(ExhaustiveChoice.Range... ranges) {
		ExhaustiveAtom exhaustiveAtom = new ExhaustiveAtom(ranges);
		if (exhaustiveAtom.maxCount() == Exhaustive.INFINITE) {
			return Optional.empty();
		}
		return Optional.of(exhaustiveAtom);
	}

	static Optional<OrAtom> or(Optional<ExhaustiveAtom>... atoms) {
		if (Arrays.stream(atoms).anyMatch(Optional::isEmpty)) {
			return Optional.empty();
		}
		List<ExhaustiveAtom> atomList = Arrays.stream(atoms).map(Optional::get).toList();
		OrAtom orAtom = new OrAtom(atomList);
		if (orAtom.maxCount() == Exhaustive.INFINITE) {
			return Optional.empty();
		}
		return Optional.of(orAtom);
	}

	static Optional<ExhaustiveList> list(int size, Optional<? extends ExhaustiveSource<?>> elementSource) {
		if (size == 0) {
			// TODO: Should be able to use any() here but type variance makes it difficult
			return Optional.of(new ExhaustiveEmptyList());
		}
		if (elementSource.isEmpty()) {
			return Optional.empty();
		}
		ExhaustiveList exhaustiveList = new ExhaustiveList(size, elementSource.get());
		if (exhaustiveList.maxCount() == Exhaustive.INFINITE) {
			return Optional.empty();
		}
		return Optional.of(exhaustiveList);
	}

	static Optional<? extends ExhaustiveSource<?>> set(
		int size,
		Optional<? extends ExhaustiveSource<?>> elementSource
	) {
		if (elementSource.isEmpty()) {
			return Optional.empty();
		}
		if (size == 0) {
			return any();
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
	static Optional<ExhaustiveTree> tree(Optional<? extends ExhaustiveSource<?>> head, Function<GenSource, Optional<? extends ExhaustiveSource<?>>> childCreator) {
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
		return (T) new RecordedSource(recording());
	}

	class AnyExhaustiveSource<T extends GenSource> extends AbstractExhaustiveSource<T> {
		/**
		 * Return an upper bound on the number of values this exhaustive source can generate.
		 * If the number of values is infinite, return {@code INFINITE}.
		 */
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
			return this;
		}
	}
}
