package jqwik2.api;

import java.util.*;

import jqwik2.api.arbitraries.*;
import jqwik2.internal.*;

public interface JqwikProperty {

	static Builder property(String propertyId) {
		return new PropertyBuilder(propertyId);
	}

	static <T1> Verifier1<T1> forAll(Arbitrary<T1> arbitrary) {
		return new PropertyBuilder().forAll(arbitrary);
	}



	interface Builder {
		<T1> Verifier1<T1> forAll(Arbitrary<T1> arbitrary);
	}

	interface Condition {
		boolean check(List<Object> params) throws Throwable;
	}

	interface Verifier1<T1> {
		JqwikProperty check(C1<T1> checker);

		JqwikProperty verify(V1<T1> verifier);
	}

	interface Verifier2<T1, T2> {
		JqwikProperty check(C2<T1, T2> checker);

		JqwikProperty verify(V2<T1, T2> verifier);
	}

	interface C1<T1> {
		boolean check(T1 v1) throws Throwable;
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

	interface C2<T1, T2> {
		boolean check(T1 v1, T2 v2) throws Throwable;
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
}
