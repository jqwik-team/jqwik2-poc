package jqwik2.api.description;

import java.util.*;

import jqwik2.api.*;
import jqwik2.api.functions.*;
import jqwik2.internal.description.*;

public interface PropertyDescription {

	static Builder property(String propertyId) {
		return new PropertyBuilder(propertyId);
	}

	static Builder property() {
		return new PropertyBuilder();
	}

	interface Builder {
		<T1> Invariant1<T1> forAll(Arbitrary<T1> arbitrary);

		<T1, T2> Invariant2<T1, T2> forAll(Arbitrary<T1> a1, Arbitrary<T2> a2);
	}

	interface Invariant1<T1> {
		PropertyDescription check(Check.C1<T1> checker);

		PropertyDescription verify(Verify.V1<T1> verifier);

		Invariant1<T1> classify(List<Classifier.Case<Check.C1<T1>>> cases);

		// TODO: Provide convenience methods for classify, eg
		// Invariant1<T1> classify(
		// C1<T1> c1, String l1, double mp1,
		// C1<T1> c2, String l2, double mp2
		// );
	}

	interface Invariant2<T1, T2> {
		PropertyDescription check(Check.C2<T1, T2> checker);

		PropertyDescription verify(Verify.V2<T1, T2> verifier);

		Invariant2<T1, T2> classify(List<Classifier.Case<Check.C2<T1, T2>>> cases);
	}

	String id();

	List<Arbitrary<?>> arbitraries();

	Condition invariant();

	int arity();

	List<Classifier> classifiers();
}
