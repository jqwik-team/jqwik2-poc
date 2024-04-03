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

}
