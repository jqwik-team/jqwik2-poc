package jqwik2.tdd;

import jqwik2.api.*;

public class TDD {

	private TDD() {}

	public static TddProperty.Builder id(String id) {
		return new TddPropertyBuilder(id);
	}

	public static <T1> TddProperty.P1<T1> forAll(Arbitrary<T1> arbitrary) {
		return new TddPropertyBuilder().forAll(arbitrary);
	}

	public static <T1, T2> TddProperty.P2<T1, T2> forAll(Arbitrary<T1> a1, Arbitrary<T2> a2) {
		return new TddPropertyBuilder().forAll(a1, a2);
	}

}
