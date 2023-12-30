package jqwik2;

import jqwik2.api.*;

public interface ExhaustiveSource extends GenSource, Exhaustive<ExhaustiveSource> {

	static ExhaustiveAtom atom(int... maxChoices) {
		return new ExhaustiveAtom(maxChoices);
	}
}
