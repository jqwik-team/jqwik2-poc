package jqwik2.tdd;

import jqwik2.api.*;
import jqwik2.api.functions.*;

public interface TddProperty<T extends TddProperty<T>> {
	interface Builder {
		<T1> P1<T1> forAll(Arbitrary<T1> arbitrary);

		<T1, T2> P2<T1, T2> forAll(Arbitrary<T1> a1, Arbitrary<T2> a2);
	}

	interface P1<T1> extends TddProperty<P1<T1>> {
		P1<T1> verifyCase(String label, Check.C1<T1> condition, Verify.V1<T1> verifier);
	}

	interface P2<T1, T2> extends TddProperty<P2<T1, T2>> {
		P2<T1, T2> verifyCase(String label, Check.C2<T1, T2> condition, Verify.V2<T1, T2> verifier);
	}

	T publisher(PlatformPublisher publisher);

	TddDrivingResult drive(TddDrivingStrategy strategy);

	default TddDrivingResult drive() {
		return drive(TddDrivingStrategy.DEFAULT);
	}
}
