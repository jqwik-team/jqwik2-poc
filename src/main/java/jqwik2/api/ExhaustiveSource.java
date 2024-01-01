package jqwik2.api;

import java.util.function.*;

import jqwik2.*;
import jqwik2.exhaustive.*;

public interface ExhaustiveSource extends GenSource, Exhaustive<ExhaustiveSource> {

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

	static ExhaustiveList list(int size, ExhaustiveSource elementSource) {
		if (size == 0) {
			return new ExhaustiveEmptyList();
		}
		return new ExhaustiveList(size, elementSource);
	}

	/**
	 * Generate tree where head is atom with cardinality 1
	 *
	 * @param range A range of all possible choices for head
	 * @param childCreator Function to create child source based on head value
	 */
	static ExhaustiveTree tree(ExhaustiveChoice.Range range, Function<Integer, ExhaustiveSource> childCreator) {
		return new ExhaustiveTree(range, childCreator);
	}

}
