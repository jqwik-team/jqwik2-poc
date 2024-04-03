package jqwik2.tdd;

import java.util.*;
import java.util.function.*;

import jqwik2.api.*;
import jqwik2.api.database.*;
import jqwik2.api.description.*;
import jqwik2.api.functions.*;
import jqwik2.api.validation.*;
import jqwik2.api.validation.PropertyValidationStrategy.*;

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
		private final Map<TddCase, TddRecord> records = new LinkedHashMap<>();

		public TddP1(Arbitrary<T1> a1) {
			this.a1 = a1;
		}

		@Override
		public TddDrivingResult drive(TddDrivingStrategy strategy) {
			List<PropertyValidationResult> caseResults = new ArrayList<>();
			for (var tddCase : cases) {
				var validator =
					PropertyValidator.forProperty(tddCase.property())
									 .failureDatabase(FailureDatabase.NULL)
									 .publisher(PlatformPublisher.NULL)
									 // .publisher(PlatformPublisher.STDOUT)
									 .registerTryExecutionListener((r, s) -> collectTddStep(tddCase, r, s));
				var result = validator.validate(buildRunConfiguration(strategy));
				caseResults.add(result);
				publishRecord(tddCase);
			}
			PropertyValidationStatus status = PropertyValidationStatus.SUCCESSFUL;
			if (caseResults.stream().anyMatch(r -> !r.isSuccessful())) {
				status = PropertyValidationStatus.FAILED;
			}

			PropertyValidationResult everythingCoveredResult = validateEverythingCovered(strategy);
			boolean isEverythingCovered = everythingCoveredResult.isSuccessful();

			if (!isEverythingCovered) {
				System.out.println("NOT ALL VALUES COVERED:");
				everythingCoveredResult.falsifiedSamples().forEach(
					s -> System.out.printf("  %s%n", s.sample().regenerateValues())
				);
			}

			return new TddDrivingResult(status, caseResults, isEverythingCovered);
		}

		private void publishRecord(TddCase tddCase) {
			var record = records.get(tddCase);
			var report = new StringBuilder();
			if (record != null) {
				record.publish(report);
			}
			PlatformPublisher.STDOUT.publish(tddCase.property().id(), report.toString());
		}

		private void collectTddStep(TddCase tddCase, TryExecutionResult r, Sample s) {
			if (r.status() == TryExecutionResult.Status.INVALID) {
				return;
			}
			updateRecord(tddCase, r, s);
		}

		private void updateRecord(TddCase tddCase, TryExecutionResult result, Sample sample) {
			var record = records.computeIfAbsent(tddCase, tddC -> new TddRecord(tddC.label()));
			record.update(result, sample);
		}

		private PropertyValidationResult validateEverythingCovered(TddDrivingStrategy strategy) {
			String everythingCoveredId = propertyId + ":everythingCoveredResult";
			Predicate<T1> everythingCovered = t1 -> cases.stream().anyMatch(c -> {
				try {
					return c.property().condition().check(List.of(t1));
				} catch (Throwable e) {
					return false;
				}
			});
			PropertyDescription property = PropertyDescription.property(everythingCoveredId)
															  .forAll(a1)
															  .check(everythingCovered::test);
			var validator = PropertyValidator.forProperty(property)
											 .failureDatabase(FailureDatabase.NULL)
											 .publisher(PlatformPublisher.NULL);
			return validator.validate(buildRunConfiguration(strategy));
		}

		private PropertyValidationStrategy buildRunConfiguration(TddDrivingStrategy strategy) {
			return PropertyValidationStrategy.builder()
											 .withGeneration(GenerationMode.GROWING)
											 .withMaxTries(strategy.maxTries())
											 .withMaxRuntime(strategy.maxRuntime())
											 .build();
		}

		@Override
		public P1<T1> verifyCase(String label, Check.C1<T1> check1, Verify.V1<T1> v1) {
			var caseId = propertyId + ":" + label.trim();
			var property = PropertyDescription.property(caseId)
											  .forAll(a1)
											  .verify(verifier(v1, check1));
			cases.add(new TddCase(label, property));
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
