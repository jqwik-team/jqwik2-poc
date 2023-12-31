package jqwik2;

import java.util.function.*;

import jqwik2.api.*;

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
		return new ExhaustiveList(size, elementSource);
	}

	static ExhaustiveTree tree(ExhaustiveAtom head, Function<Integer[], ExhaustiveSource> childCreator) {
		return new ExhaustiveTree(head, childCreator);
	}
}
