package jqwik2;

import jqwik2.api.*;

public interface ExhaustiveSource extends GenSource, Exhaustive<ExhaustiveSource> {

	static ExhaustiveAtom atom(int... maxChoices) {
		return new ExhaustiveAtom(maxChoices);
	}

	static ExhaustiveAtom atom(ExhaustiveChoice.Range...ranges) {
		return new ExhaustiveAtom(ranges);
	}

	static ExhaustiveChoice.Range range(int min, int maxExcluded) {
		return new ExhaustiveChoice.Range(min, maxExcluded);
	}

	static ExhaustiveChoice.Range value(int value) {
		return range(value, value + 1);
	}

	static OrAtom or(ExhaustiveAtom ... atoms) {
		return new OrAtom(atoms);
	}
}
