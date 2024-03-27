package jqwik2.tdd;

import jqwik2.api.*;
import jqwik2.api.description.*;

public interface TddProperty {
	interface Builder {
		<T1> P1<T1> forAll(Arbitrary<T1> arbitrary);
	}

	interface P1<T1> extends TddProperty {
		P1<T1> verifyCase(String label, PropertyDescription.C1<T1> condition, PropertyDescription.V1<T1> verifier);
		// PropertyDescription check(PropertyDescription.C1<T1> checker);
		//
		// PropertyDescription verify(PropertyDescription.V1<T1> verifier);
	}

	TddResult drive();
}
