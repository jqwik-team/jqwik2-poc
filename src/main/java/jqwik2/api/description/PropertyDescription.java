package jqwik2.api.description;

import java.util.*;

import jqwik2.api.*;
import jqwik2.internal.description.*;

public interface PropertyDescription {

	static Builder property(String propertyId) {
		return new PropertyBuilder(propertyId);
	}

	static PropertyBuilder property() {
		return new PropertyBuilder();
	}

	interface Builder {
		<T1> Verifier1<T1> forAll(Arbitrary<T1> arbitrary);

		<T1, T2> Verifier2<T1, T2> forAll(Arbitrary<T1> a1, Arbitrary<T2> a2);
	}

	interface Verifier1<T1> {
		PropertyDescription check(C1<T1> checker);

		PropertyDescription verify(V1<T1> verifier);

		Verifier1<T1> classify(List<Classifier.Case<C1<T1>>> cases);

		// TODO: Provide convenience methods for classify, eg
		// Verifier1<T1> classify(
		// C1<T1> c1, String l1, double mp1,
		// C1<T1> c2, String l2, double mp2
		// );
	}

	interface Verifier2<T1, T2> {
		PropertyDescription check(C2<T1, T2> checker);

		PropertyDescription verify(V2<T1, T2> verifier);

		Verifier2<T1, T2> classify(List<Classifier.Case<C2<T1, T2>>> cases);
	}

	interface Check<C extends Check<C>> {
		Condition asCondition();
	}

	interface C1<T1> extends Check<C1<T1>> {
		boolean check(T1 v1) throws Throwable;

		@SuppressWarnings("unchecked")
		default Condition asCondition() {
			return args -> this.check((T1) args.get(0));
		}
	}

	interface V1<T1> {
		void verify(T1 v1) throws Throwable;

		default C1<T1> asCheck() {
			return v1 -> {
				verify(v1);
				return true;
			};
		}
	}

	interface C2<T1, T2> extends Check<C2<T1, T2>> {
		boolean check(T1 v1, T2 v2) throws Throwable;

		@SuppressWarnings("unchecked")
		default Condition asCondition() {
			return args -> this.check((T1) args.get(0), (T2) args.get(1));
		}

	}

	interface V2<T1, T2> {
		void verify(T1 v1, T2 v2) throws Throwable;

		default C2<T1, T2> asCheck() {
			return (v1, v2) -> {
				verify(v1, v2);
				return true;
			};
		}
	}

	String id();

	List<Arbitrary<?>> arbitraries();

	Condition condition();

	int arity();

	List<Classifier> classifiers();
}
