package jqwik2.api;

import java.util.function.*;

import jqwik2.api.recording.*;
import jqwik2.internal.exhaustive.*;
import jqwik2.internal.recording.*;

public interface ExhaustiveSource<T extends GenSource> extends Exhaustive<ExhaustiveSource<T>>, Supplier<T> {

	static ExhaustiveAtom atom(int... maxChoices) {
		return new ExhaustiveAtom(maxChoices);
	}

	static ExhaustiveAtom atom(ExhaustiveChoice.Range...ranges) {
		return new ExhaustiveAtom(ranges);
	}

	static ExhaustiveChoice.Range range(int min, int max) {
		return new ExhaustiveChoice.Range(min, max);
	}

	static ExhaustiveChoice.Range value(int value) {
		return range(value, value);
	}

	static OrAtom or(ExhaustiveAtom ... atoms) {
		return new OrAtom(atoms);
	}

	static ExhaustiveList list(int size, ExhaustiveSource<?> elementSource) {
		if (size == 0) {
			return new ExhaustiveEmptyList();
		}
		return new ExhaustiveList(size, elementSource);
	}

	/**
	 * Generate tree from given head source and child creator function
	 *
	 * @param head Source for head value
	 * @param childCreator Function to create child source based on head value
	 */
	static ExhaustiveTree tree(ExhaustiveSource<?> head, Function<GenSource, ExhaustiveSource<?>> childCreator) {
		return new ExhaustiveTree(head, childCreator);
	}

	Recording recording();

	@SuppressWarnings("unchecked")
	default T get() {
		return (T) new RecordedSource(recording());
	}
}
