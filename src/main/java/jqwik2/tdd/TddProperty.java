package jqwik2.tdd;

import java.util.*;

import jqwik2.api.*;
import jqwik2.api.description.*;

public interface TddProperty {
	static Builder id(String id) {
		return new TddPropertyBuilder(id);
	}

	interface Builder {
		<T1> TddProperty1<T1> forAll(Arbitrary<T1> arbitrary);
	}

	interface TddProperty1<T1> extends TddProperty {
		// PropertyDescription check(PropertyDescription.C1<T1> checker);
		//
		// PropertyDescription verify(PropertyDescription.V1<T1> verifier);
	}

	TddResult drive();
}
