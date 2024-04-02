package jqwik2.tdd;

import java.util.*;
import java.util.function.*;

import jqwik2.api.*;
import jqwik2.api.description.*;
import jqwik2.api.functions.*;
import jqwik2.api.validation.*;
import jqwik2.internal.*;

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
		private final List<Pair<String, PropertyDescription>> cases = new ArrayList<>();
		private final Map<String, TddRecord> records = new LinkedHashMap<>();

		public TddP1(Arbitrary<T1> a1) {
			this.a1 = a1;
		}

		@Override
		public TddResult drive() {
			List<PropertyValidationResult> caseResults = new ArrayList<>();
			for (var tddCase : cases) {
				var validator =
					PropertyValidator.forProperty(tddCase.second())
									 .publisher(PlatformPublisher.NULL)
									 // .publisher(PlatformPublisher.STDOUT)
									 .registerTryExecutionListener((r, s) -> collectTddStep(tddCase, r, s));
				var result = validator.validate(buildRunConfiguration());
				caseResults.add(result);
				publishRecord(tddCase);
			}
			PropertyValidationStatus status = PropertyValidationStatus.SUCCESSFUL;
			if (caseResults.stream().anyMatch(r -> !r.isSuccessful())) {
				status = PropertyValidationStatus.FAILED;
			}

			PropertyValidationResult everythingCoveredResult = validateEverythingCovered();
			boolean isEverythingCovered = everythingCoveredResult.isSuccessful();

			if (!isEverythingCovered) {
				System.out.println("NOT ALL VALUES COVERED:");
				everythingCoveredResult.falsifiedSamples().forEach(
					s -> System.out.printf("  %s%n", s.sample().regenerateValues())
				);
			}

			return new TddResult(status, caseResults, isEverythingCovered);
		}

		private void publishRecord(Pair<String, PropertyDescription> tddCase) {
			var record = records.get(tddCase.first());
			var report = new StringBuilder();
			if (record != null) {
				record.publish(report);
			}
			PlatformPublisher.STDOUT.publish(tddCase.second().id(), report.toString());
		}

		private void collectTddStep(Pair<String, PropertyDescription> tddCase, TryExecutionResult r, Sample s) {
			if (r.status() == TryExecutionResult.Status.INVALID) {
				return;
			}
			updateRecord(tddCase, r, s);
		}

		private void updateRecord(
			Pair<String, PropertyDescription> tddCase,
			TryExecutionResult result,
			Sample sample
		) {
			var record = records.computeIfAbsent(tddCase.first(), TddRecord::new);
			record.update(result, sample);
		}

		private PropertyValidationResult validateEverythingCovered() {
			String everythingCoveredId = propertyId + ":everythingCoveredResult";
			Predicate<T1> everythingCovered = t1 -> cases.stream().anyMatch(c -> {
				try {
					return c.second().condition().check(List.of(t1));
				} catch (Throwable e) {
					return false;
				}
			});
			PropertyDescription property = PropertyDescription.property(everythingCoveredId)
															  .forAll(a1)
															  .check(everythingCovered::test);
			var validator = PropertyValidator.forProperty(property)
											 .publisher(PlatformPublisher.NULL);
			return validator.validate(buildRunConfiguration());
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
			cases.add(Pair.of(label, property));
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
