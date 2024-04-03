package jqwik2.tdd;

import jqwik2.api.*;
import jqwik2.api.functions.*;

import static jqwik2.tdd.TddDrivingStrategy.*;

public interface TddProperty {
	interface Builder {
		<T1> P1<T1> forAll(Arbitrary<T1> arbitrary);
	}

	interface P1<T1> extends TddProperty {
		P1<T1> verifyCase(String label, Check.C1<T1> condition, Verify.V1<T1> verifier);
		// PropertyDescription check(PropertyDescription.C1<T1> checker);
		//
		// PropertyDescription verify(PropertyDescription.V1<T1> verifier);
	}

	TddDrivingResult drive(TddDrivingStrategy strategy);

	default TddDrivingResult drive() {
		return drive(TddDrivingStrategy.DEFAULT);
	}
}
