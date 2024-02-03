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

	static Optional<ExhaustiveSource<?>> atom(int maxChoiceIncluded) {
		ExhaustiveAtom exhaustiveAtom = new ExhaustiveAtom(maxChoiceIncluded);
		if (exhaustiveAtom.maxCount() == Exhaustive.INFINITE) {
			return Optional.empty();
		}
		return Optional.of(exhaustiveAtom);
	}

	static Optional<ExhaustiveSource<?>> atom(ExhaustiveChoice.Range includedRange) {
		ExhaustiveAtom exhaustiveAtom = new ExhaustiveAtom(includedRange);
		if (exhaustiveAtom.maxCount() == Exhaustive.INFINITE) {
			return Optional.empty();
		}
		return Optional.of(exhaustiveAtom);
	}

	@SafeVarargs
	static Optional<ExhaustiveSource<?>> or(Optional<ExhaustiveSource<?>>... alternatives) {
		if (Arrays.stream(alternatives).anyMatch(Optional::isEmpty)) {
			return Optional.empty();
		}
		@SuppressWarnings("OptionalGetWithoutIsPresent") // It's checked above
		List<? extends ExhaustiveSource<?>> atomList = Arrays.stream(alternatives).map(Optional::get).toList();
		ExhaustiveOr exhaustiveOr = new ExhaustiveOr(atomList);
		if (exhaustiveOr.maxCount() == Exhaustive.INFINITE) {
			return Optional.empty();
		}
		return Optional.of(exhaustiveOr);
	}

	@SafeVarargs
	static Optional<ExhaustiveSource<?>> tuple(Optional<ExhaustiveSource<?>> ... valueSources) {
		if (Arrays.stream(valueSources).anyMatch(Optional::isEmpty)) {
			return Optional.empty();
		}
		@SuppressWarnings("OptionalGetWithoutIsPresent") // It's checked above
		List<? extends ExhaustiveSource<?>> values = Arrays.stream(valueSources).map(Optional::get).toList();
		ExhaustiveTuple exhaustiveList = new ExhaustiveTuple(values);
		if (exhaustiveList.maxCount() == Exhaustive.INFINITE) {
			return Optional.empty();
		}
		return Optional.of(exhaustiveList);
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
	 * Generate an exhaustive source that reflects flat mapping one exhaustive source to another
	 *
	 * @param from         Source for from value
	 * @param mappingFunction Function to create child source based on from value
	 */
	static Optional<ExhaustiveSource<?>> flatMap(Optional<ExhaustiveSource<?>> from, Function<GenSource, Optional<ExhaustiveSource<?>>> mappingFunction) {
		if (from.isEmpty()) {
			return Optional.empty();
		}
		ExhaustiveFlatMap exhaustiveFlatMap = new ExhaustiveFlatMap(from.get(), mappingFunction);
		if (exhaustiveFlatMap.maxCount() == Exhaustive.INFINITE) {
			return Optional.empty();
		}
		return Optional.of(exhaustiveFlatMap);
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
			return Recording.EMPTY;
		}

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
