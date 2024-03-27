package jqwik2.tdd;

import java.util.*;
import java.util.function.*;

import jqwik2.api.*;
import jqwik2.api.description.*;

class TddPropertyBuilder implements TddProperty.Builder {
	private final String propertyId;

	TddPropertyBuilder(String propertyId) {
		this.propertyId = propertyId;
	}

	@Override
	public <T1> TddProperty.P1<T1> forAll(Arbitrary<T1> arbitrary) {
		return new TddP1<>(arbitrary);
	}

	private class TddP1<T1> implements TddProperty.P1<T1> {
		private final Arbitrary<T1> a1;
		private final List<TddCase> cases = new ArrayList<>();

		public TddP1(Arbitrary<T1> a1) {
			this.a1 = a1;
		}

		@Override
		public TddResult drive() {
			return new TddResult(TddResult.Status.NOT_COVERED);
		}

		@Override
		public P1<T1> verifyCase(String label, PropertyDescription.C1<T1> check1, PropertyDescription.V1<T1> v1) {

			Condition condition = check1.asCondition();
			Consumer<List<Object>> verifier = params -> {
				try {
					v1.verify((T1) params.get(0));
				} catch (Throwable throwable) {
					throw new AssertionError("Verification failed: " + throwable.getMessage(), throwable);
				}
			};
			cases.add(new TddCase(label, condition, verifier));
			return this;
		}
	}
}
