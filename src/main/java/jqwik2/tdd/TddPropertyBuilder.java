package jqwik2.tdd;

import jqwik2.api.*;

class TddPropertyBuilder implements TddProperty.Builder {
	private final String propertyId;

	TddPropertyBuilder(String propertyId) {
		this.propertyId = propertyId;
	}

	@Override
	public <T1> TddProperty.TddProperty1<T1> forAll(Arbitrary<T1> arbitrary) {
		return new TddP1<>(arbitrary);
	}

	private class TddP1<T1> implements TddProperty.TddProperty1<T1> {
		public TddP1(Arbitrary<T1> a1) {}

		@Override
		public TddResult drive() {
			return new TddResult(TddResult.Status.NOT_COVERED);
		}
	}
}
