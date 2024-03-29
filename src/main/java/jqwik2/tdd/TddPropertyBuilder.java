package jqwik2.tdd;

import java.util.*;

import jqwik2.api.*;
import jqwik2.api.description.*;
import jqwik2.api.functions.*;
import jqwik2.api.validation.*;

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
		private final List<PropertyDescription> cases = new ArrayList<>();

		public TddP1(Arbitrary<T1> a1) {
			this.a1 = a1;
		}

		@Override
		public TddResult drive() {
			List<PropertyValidationResult> caseResults = new ArrayList<>();
			for (var tddCase : cases) {
				var validator = PropertyValidator.forProperty(tddCase);
				var result = validator.validate(buildRunConfiguration());
				caseResults.add(result);
			}
			TddResult.Status status = TddResult.Status.SUCCESSFUL;
			if (caseResults.stream().anyMatch(r -> !r.isSuccessful())) {
				status = TddResult.Status.FAILED;
			}
			// TODO: Discover Status.NOT_COVERED
			return new TddResult(status, caseResults);
		}

		private PropertyValidationStrategy buildRunConfiguration() {
			return PropertyValidationStrategy.builder()
											 .withGeneration(PropertyValidationStrategy.GenerationMode.GROWING)
											 .withMaxTries(1000)
											 .withShrinking(PropertyValidationStrategy.ShrinkingMode.OFF)
											 .build();
		}

		@Override
		public P1<T1> verifyCase(String label, Check.C1<T1> check1, Verify.V1<T1> v1) {
			var caseId = propertyId + ":" + label.trim();
			var property = PropertyDescription.property(caseId)
											  .forAll(a1)
											  .verify(verifier(v1, check1));
			cases.add(property);
			return this;
		}

		private Verify.V1<T1> verifier(Verify.V1<T1> v1, Check.C1<T1> check) {
			return t1 -> {
				Assume.that(check.check(t1));
				v1.verify(t1);
			};
		}
	}
}
