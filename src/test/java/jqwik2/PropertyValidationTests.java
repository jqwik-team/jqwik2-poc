package jqwik2;

import java.time.*;
import java.util.*;
import java.util.concurrent.atomic.*;

import jqwik2.api.Arbitrary;
import jqwik2.api.Assume;
import jqwik2.api.*;
import jqwik2.api.arbitraries.*;
import jqwik2.api.database.*;
import jqwik2.api.description.*;
import jqwik2.api.recording.*;
import jqwik2.api.validation.*;
import jqwik2.internal.*;
import org.mockito.*;
import org.opentest4j.*;

import net.jqwik.api.*;
import net.jqwik.api.lifecycle.*;

import static jqwik2.api.description.Classifier.*;
import static jqwik2.api.recording.Recording.list;
import static jqwik2.api.recording.Recording.*;
import static jqwik2.api.validation.PropertyValidationStrategy.GenerationMode.*;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class PropertyValidationTests {

	@BeforeProperty
	void resetFailureDatabase() {
		JqwikDefaults.defaultFailureDatabase().clear();
	}

	@Example
	void checkWith1ParameterSucceeds() {
		var property = PropertyDescription.property().forAll(Numbers.integers()).check(i -> {
			assertThat(i).isInstanceOf(Integer.class);
			return true;
		});

		PropertyValidator validator = PropertyValidator.forProperty(property);
		PropertyValidationResult result = validator.validate();

		assertThat(result.isSuccessful()).isTrue();
		assertThat(result.countTries()).isEqualTo(100);
		assertThat(result.countChecks()).isEqualTo(100);
	}

	@Example
	void verifyWith1ParameterSucceeds() {
		var property = PropertyDescription.property().forAll(Numbers.integers()).verify(i -> {
			assertThat(i).isInstanceOf(Integer.class);
		});

		PropertyValidator validator = PropertyValidator.forProperty(property);
		PropertyValidationResult result = validator.validate();

		assertThat(result.isSuccessful()).isTrue();
		assertThat(result.failure()).isEmpty();
		assertThat(result.countTries()).isEqualTo(100);
		assertThat(result.countChecks()).isEqualTo(100);
	}

	@Example
	void checkWithAssumptionSucceeds() {
		var property = PropertyDescription.property().forAll(Numbers.integers()).check(i -> {
			Assume.that(i % 2 == 0);
			return i instanceof Integer;
		});
		PropertyValidationResult result = PropertyValidator.forProperty(property).validate();

		assertThat(result.isSuccessful()).isTrue();
		assertThat(result.failure()).isEmpty();
		assertThat(result.countTries()).isEqualTo(100);
		assertThat(result.countChecks()).isBetween(20, 80);
	}

	@Example
	void verifyWithAssumptionSucceeds() {
		var property = PropertyDescription.property().forAll(Numbers.integers()).verify(i -> {
			Assume.that(i % 2 == 0);
			assertThat(i).isInstanceOf(Integer.class);
		});
		PropertyValidationResult result = PropertyValidator.forProperty(property).validate();

		assertThat(result.isSuccessful()).isTrue();
		assertThat(result.countTries()).isEqualTo(100);
		assertThat(result.countChecks()).isBetween(20, 80);
	}

	@Example
	void checkWith1ParameterFails() {
		var property = PropertyDescription.property("cp")
										  .forAll(Numbers.integers()).check(i -> false);

		PropertyValidationResult result = PropertyValidator.forProperty(property).validate();

		assertThat(result.isFailed()).isTrue();
		assertThat(result.failure()).isPresent();
		result.failure().ifPresent(failure -> assertThat(failure).isInstanceOf(AssertionFailedError.class));
		assertThat(result.countTries()).isEqualTo(1);
		assertThat(result.countChecks()).isEqualTo(1);

		// Shrinking usually reduces the initial falsified sample to a minimal falsified sample
		assertThat(result.falsifiedSamples()).hasSizeGreaterThanOrEqualTo(1);
	}

	@Example
	void verifyWith1ParameterFails() {
		var failure = new AssertionError("failed");
		var property = PropertyDescription.property("cp")
										  .forAll(Numbers.integers())
										  .verify(i -> {
											  throw failure;
										  });

		PropertyValidationResult result = PropertyValidator.forProperty(property).validate();

		assertThat(result.isFailed()).isTrue();
		assertThat(result.failure()).hasValue(failure);
		assertThat(result.countTries()).isEqualTo(1);
		assertThat(result.countChecks()).isEqualTo(1);

		// Shrinking usually reduces the initial falsified sample to a minimal falsified sample
		assertThat(result.falsifiedSamples()).hasSizeGreaterThanOrEqualTo(1);
	}

	@Example
	void failingPropertyThrowsExceptionWith_throwIfNotSuccessful() {
		var property = PropertyDescription.property().forAll(Values.just(42)).check(i -> false);
		var result = PropertyValidator.forProperty(property).validate();

		assertThatThrownBy(
			() -> result.throwIfNotSuccessful()
		).isInstanceOf(AssertionError.class)
		 .hasMessageContaining("Property check failed");
	}

	@Example
	void abortedPropertyThrowsExceptionWith_throwIfNotSuccessful() {
		var throwingArbitrary = new Arbitrary<Integer>() {
			@Override
			public Generator<Integer> generator() {
				return source -> {
					throw new TestAbortedException("Property aborted because of thrown exception");
				};
			}
		};

		var property = PropertyDescription.property().forAll(throwingArbitrary).check(i -> true);
		var result = PropertyValidator.forProperty(property).validate();

		assertThatThrownBy(
			() -> result.throwIfNotSuccessful()
		).isInstanceOf(TestAbortedException.class)
		 .hasMessageContaining("Property aborted because of thrown exception");
	}

	@Example
	void verifyWith2ParametersSucceeds() {
		var property = PropertyDescription.property().forAll(
			Values.just(1),
			Values.just(2)
		).verify((i1, i2) -> {
			assertThat(i1).isEqualTo(1);
			assertThat(i2).isEqualTo(2);
		});

		var strategy = PropertyValidationStrategy.builder()
												 .withGeneration(RANDOMIZED)
												 .build();

		var result = PropertyValidator.forProperty(property).validate(strategy);

		assertThat(result.isSuccessful()).isTrue();
		assertThat(result.countTries()).isEqualTo(100);
		assertThat(result.countChecks()).isEqualTo(100);
	}

	@Example
	void verifyWith2ParametersFails() {
		var property = PropertyDescription.property().forAll(
			Values.just(1),
			Values.just(2)
		).verify((i1, i2) -> {
			fail("failed");
		});

		var result = PropertyValidator.forProperty(property).validate();
		assertThat(result.isFailed()).isTrue();
	}

	@Example
	void edgeCasesMode_MIXIN() {
		PropertyValidationStrategy strategy =
			PropertyValidationStrategy.builder()
									  .withMaxTries(1000)
									  .withMaxRuntime(Duration.ofMinutes(10))
									  .withFilterOutDuplicateSamples(false)
									  .withSeedSupplier(RandomChoice::generateRandomSeed)
									  .withShrinking(PropertyValidationStrategy.ShrinkingMode.OFF)
									  .withGeneration(RANDOMIZED)
									  .withEdgeCases(PropertyValidationStrategy.EdgeCasesMode.MIXIN)
									  .withAfterFailure(PropertyValidationStrategy.AfterFailureMode.SAMPLES_ONLY)
									  .withConcurrency(PropertyValidationStrategy.ConcurrencyMode.SINGLE_THREAD)
									  .build();

		List<Integer> values = Collections.synchronizedList(new ArrayList<>());
		var property = PropertyDescription.property().forAll(Numbers.integers()).verify(values::add);

		PropertyValidationResult resultExhaustive = PropertyValidator.forProperty(property).validate(strategy);
		assertThat(resultExhaustive.isSuccessful()).isTrue();
		assertThat(resultExhaustive.countTries()).isEqualTo(1000);

		assertThat(values).contains(0, 1, -1, Integer.MAX_VALUE, Integer.MIN_VALUE);
	}

	@Example
	void concurrencyMode_CACHED_THREAD_POOL() {
		var strategy = PropertyValidationStrategy.builder()
												 .withConcurrency(PropertyValidationStrategy.ConcurrencyMode.CACHED_THREAD_POOL)
												 .withMaxTries(1000)
												 .build();

		var property = PropertyDescription.property().forAll(Numbers.integers()).verify(i -> {
			ConcurrentRunnerTests.sleepInThread(10);
		});

		// Takes > 10 secs in single thread mode, but only 1 sec in cached thread pool mode
		var initialResult = PropertyValidator.forProperty(property).validate(strategy);
		assertThat(initialResult.isSuccessful()).isTrue();
		assertThat(initialResult.countTries()).isEqualTo(1000);
	}

	@Example
	void maxTriesSetTo0_runsUntilTimeout() {
		var strategy = PropertyValidationStrategy.builder()
												 .withMaxTries(0)
												 .withMaxRuntime(Duration.ofSeconds(1))
												 .build();

		var property = PropertyDescription.property().forAll(Numbers.integers())
										  .verify(i -> {});

		// Should take about 1 second till maxRuntime is reached
		var checkingResult = PropertyValidator.forProperty(property).validate(strategy);
		assertThat(checkingResult.isSuccessful()).isTrue();
		assertThat(checkingResult.countTries()).isGreaterThan(1);
		assertThat(checkingResult.countChecks()).isEqualTo(checkingResult.countTries());
	}

	@Example
	void maxDurationSetTo0_runsUntilMaxTriesIsReached() {
		var strategy = PropertyValidationStrategy.builder()
												 .withMaxTries(10)
												 .withMaxRuntime(Duration.ZERO)
												 .build();

		var checkingProperty = PropertyDescription.property().forAll(Numbers.integers())
												  .verify(i -> Thread.sleep(100));

		// Should take about 1 second (10 * 100ms)
		var checkingResult = PropertyValidator.forProperty(checkingProperty).validate(strategy);
		assertThat(checkingResult.isSuccessful()).isTrue();
		assertThat(checkingResult.countTries()).isEqualTo(10);
		assertThat(checkingResult.countChecks()).isEqualTo(checkingResult.countTries());
	}

	@Example
	void failedTryWillStopEndlessRunningProperty() {
		var strategy = PropertyValidationStrategy.builder()
												 .withGeneration(RANDOMIZED)
												 .withAfterFailure(PropertyValidationStrategy.AfterFailureMode.REPLAY)
												 .withMaxTries(0)
												 .withMaxRuntime(Duration.ZERO)
												 .build();

		AtomicInteger counter = new AtomicInteger();
		var property = PropertyDescription.property().forAll(Numbers.integers())
										  .verify(i -> {
											  counter.incrementAndGet();
											  try {
												  Thread.sleep(10);
											  } catch (InterruptedException ignore) {
												  // Within a test run the thread can sometimes be interrupted
											  }
											  assertThat(counter.get()).isLessThan(100);
										  });

		// Should take about 1 second (100 * 10ms)
		var checkingResult = PropertyValidator.forProperty(property).validate(strategy);

		assertThat(checkingResult.isFailed()).isTrue();
		assertThat(checkingResult.countTries()).isEqualTo(100);
		assertThat(checkingResult.countChecks()).isEqualTo(checkingResult.countTries());
	}

	@Group
	class Classifiers {

		@Example
		void classificationAccepted()  {
			var strategy = PropertyValidationStrategy.builder()
													 .withMaxTries(0)
													 .withMaxRuntime(Duration.ofSeconds(1))
													 .build();
			PropertyDescription property =
				PropertyDescription.property()
								   .forAll(Numbers.integers())
								   .classify(List.of(
									   caseOf(i -> i > 0, "positive", 45.0),
									   caseOf(i -> i < 0, "negative", 45.0),
									   caseOf(i -> i == 0, "zero")
								   ))
								   .check(i -> true);

			PropertyValidationResult result = PropertyValidator.forProperty(property).validate(strategy);
			assertThat(result.isSuccessful()).isTrue();
		}

		@Example
		void classificationRejected()  {
			var strategy = PropertyValidationStrategy.builder()
													 .withMaxTries(0)
													 .withMaxRuntime(Duration.ofSeconds(1))
													 .build();

			PropertyDescription property =
				PropertyDescription.property()
								   .forAll(Numbers.integers())
								   .classify(List.of(
									   caseOf(i -> i > 0, "positive", 55.0)
								   ))
								   .check(i -> true);

			PropertyValidationResult result = PropertyValidator.forProperty(property).validate(strategy);
			assertThat(result.isFailed()).isTrue();
		}

		@Example
		void withoutMinPercentages_plainStrategyIsUsed()  {
			var strategy = PropertyValidationStrategy.builder()
													 .withMaxTries(999)
													 .withMaxRuntime(Duration.ofSeconds(1))
													 .build();
			PropertyDescription property =
				PropertyDescription.property()
								   .forAll(Numbers.integers())
								   .classify(List.of(
									   caseOf(i -> i > 0, "positive"),
									   caseOf(i -> i < 0, "negative"),
									   caseOf(i -> i == 0, "zero")
								   ))
								   .check(i -> true);

			PropertyValidationResult result = PropertyValidator.forProperty(property).validate(strategy);
			assertThat(result.isSuccessful()).isTrue();
			assertThat(result.countChecks()).isEqualTo(999);
		}

	}

	@Group
	class FailureDatabaseInteractions {

		FailureDatabase database = mock(FailureDatabase.class);

		@Example
		void failedPropertyRunWillBeSavedToFailureDatabase() {
			var property = PropertyDescription.property("myId").forAll(Numbers.integers()).check(i -> false);

			var validator = PropertyValidator.forProperty(property);
			validator.failureDatabase(database);
			validator.validate();

			verify(database).saveFailure(Mockito.eq("myId"), anyString(), anySet());
		}

		@Example
		void successfulPropertyRunWillBeRemovedFromFailureDatabase() {
			var property = PropertyDescription.property("myId").forAll(Numbers.integers()).check(i -> true);

			var validator = PropertyValidator.forProperty(property);
			validator.failureDatabase(database);
			validator.validate();

			verify(database).deleteProperty("myId");
		}
	}

	@Group
	class AfterFailureModes {

		@Example
		void REPLAY() {
			var strategy = PropertyValidationStrategy.builder()
													 .withAfterFailure(PropertyValidationStrategy.AfterFailureMode.REPLAY)
													 .build();

			var integers = Numbers.integers().between(-1000, 1000);
			List<Integer> triedValues = new ArrayList<>();
			var property = PropertyDescription.property("idFor_REPLAY").forAll(integers).check(i -> {
				triedValues.add(i);
				return i > -10 && i < 10;
			});

			var initialResult = PropertyValidator.forProperty(property).validate(strategy);
			assertThat(initialResult.isFailed()).isTrue();
			assertThat(initialResult.countTries()).isGreaterThan(0);

			List<Integer> initiallyTriedValues = new ArrayList<>(triedValues);

			triedValues.clear();
			var replayedResult = PropertyValidator.forProperty(property).validate(strategy);
			assertThat(replayedResult).isEqualTo(initialResult);

			assertThat(triedValues).isEqualTo(initiallyTriedValues);
		}

		@Example
		void SAMPLES_ONLY() {
			var strategy = PropertyValidationStrategy.builder()
													 .withAfterFailure(PropertyValidationStrategy.AfterFailureMode.SAMPLES_ONLY)
													 .build();

			String idForSamplesOnly = "idFor_SAMPLES_ONLY";

			var integers = Numbers.integers().between(-1000, 1000);
			var property = PropertyDescription.property(idForSamplesOnly).forAll(integers)
											  .check(i -> {
												  return i > -10 && i < 10;
											  });

			var initialResult = PropertyValidator.forProperty(property).validate(strategy);
			assertThat(initialResult.isFailed()).isTrue();
			List<Integer> falsifiedValues = initialResult.falsifiedSamples().stream()
														 .map(s -> (Integer) s.sample().values().get(0))
														 .toList();
			assertThat(falsifiedValues).hasSize(2);

			List<Integer> samplesOnlyValues = new ArrayList<>();
			var propertyForReplay = PropertyDescription.property(idForSamplesOnly).forAll(integers)
													   .check(i -> {
														   samplesOnlyValues.add(i);
														   return true; // In order to try all previously falsified samples
													   });
			var replayedResult = PropertyValidator.forProperty(propertyForReplay).validate(strategy);
			assertThat(replayedResult.countTries()).isEqualTo(2);
			assertThat(samplesOnlyValues).isEqualTo(falsifiedValues);
		}
	}

	@Group
	class GenerationModes {

		@Example
		void generationMode_EXHAUSTIVE() {
			PropertyValidationStrategy strategy =
				PropertyValidationStrategy.builder()
										  .withMaxTries(100)
										  .withMaxRuntime(Duration.ofMinutes(10))
										  .withGeneration(PropertyValidationStrategy.GenerationMode.EXHAUSTIVE)
										  .withConcurrency(PropertyValidationStrategy.ConcurrencyMode.SINGLE_THREAD)
										  .build();

			var property = PropertyDescription.property().forAll(
				Numbers.integers().between(0, 3),
				Numbers.integers().between(1, 2)
			).verify((i1, i2) -> {
				assertThat(i1).isBetween(0, 3);
				assertThat(i2).isBetween(1, 2);
			});

			var result = PropertyValidator.forProperty(property).validate(strategy);
			assertThat(result.isSuccessful()).isTrue();
			assertThat(result.countTries()).isEqualTo(8);
			assertThat(result.countChecks()).isEqualTo(8);
		}

		@Example
		void generationMode_SMART_chooseExhaustive() {
			PropertyValidationStrategy strategy =
				PropertyValidationStrategy.builder()
										  .withMaxTries(100)
										  .withMaxRuntime(Duration.ofMinutes(10))
										  .withFilterOutDuplicateSamples(false)
										  .withSeedSupplier(RandomChoice::generateRandomSeed)
										  .withGeneration(PropertyValidationStrategy.GenerationMode.SMART)
										  .withConcurrency(PropertyValidationStrategy.ConcurrencyMode.SINGLE_THREAD)
										  .build();

			var exhaustiveProperty = PropertyDescription.property().forAll(
				Numbers.integers().between(0, 3),
				Numbers.integers().between(1, 2)
			).verify((i1, i2) -> {
				assertThat(i1).isBetween(0, 3);
				assertThat(i2).isBetween(1, 2);
			});

			var resultExhaustive = PropertyValidator.forProperty(exhaustiveProperty).validate(strategy);
			assertThat(resultExhaustive.isSuccessful()).isTrue();
			assertThat(resultExhaustive.countTries()).isEqualTo(8);
		}

		@Example
		void generationMode_SMART_chooseRandomize() {
			PropertyValidationStrategy strategy =
				PropertyValidationStrategy.builder()
										  .withMaxTries(100)
										  .withMaxRuntime(Duration.ofMinutes(10))
										  .withFilterOutDuplicateSamples(false)
										  .withSeedSupplier(RandomChoice::generateRandomSeed)
										  .withGeneration(PropertyValidationStrategy.GenerationMode.SMART)
										  .withConcurrency(PropertyValidationStrategy.ConcurrencyMode.SINGLE_THREAD)
										  .build();

			var randomizedProperty = PropertyDescription.property().forAll(
				Numbers.integers().between(1, 100),
				Numbers.integers().between(0, 1)
			).verify((i1, i2) -> {});

			var resultRandomized = PropertyValidator.forProperty(randomizedProperty).validate(strategy);
			assertThat(resultRandomized.isSuccessful()).isTrue();
			assertThat(resultRandomized.countTries()).isEqualTo(100);
		}

		@Example
		void generationMode_SAMPLES() {
			var integers = Numbers.integers();
			var generator = SampleGenerator.from(integers.generator());
			var randomSampleSource = SampleSource.of(new RandomGenSource());
			List<SampleRecording> sampleRecordings = new ArrayList<>();
			List<Integer> sampleValues = new ArrayList<>();
			for (int i = 0; i < 10; i++) {
				generator.generate(randomSampleSource).ifPresent(sample -> {
					sampleRecordings.add(sample.recording());
					sampleValues.add((Integer) sample.values().get(0));
				});
			}

			// Add non-fitting recording, which should be ignored
			sampleRecordings.add(new SampleRecording(list(choice(0))));

			PropertyValidationStrategy strategy =
				PropertyValidationStrategy.builder()
										  .withMaxTries(1000)
										  .withMaxRuntime(Duration.ofMinutes(10))
										  .withFilterOutDuplicateSamples(true)
										  .withSamples(sampleRecordings)
										  .withShrinking(PropertyValidationStrategy.ShrinkingMode.OFF)
										  .withGeneration(PropertyValidationStrategy.GenerationMode.SAMPLES)
										  .withConcurrency(PropertyValidationStrategy.ConcurrencyMode.SINGLE_THREAD)
										  .build();

			List<Integer> values = Collections.synchronizedList(new ArrayList<>());
			var property = PropertyDescription.property().forAll(integers).verify(values::add);
			var resultSamples = PropertyValidator.forProperty(property).validate(strategy);

			resultSamples.throwIfNotSuccessful();

			assertThat(resultSamples.isSuccessful()).isTrue();
			assertThat(resultSamples.countTries()).isEqualTo(sampleValues.size());
			assertThat(values).isEqualTo(sampleValues);
		}

		@Example
		void generationMode_GROWING() {
			PropertyValidationStrategy strategy =
				PropertyValidationStrategy.builder()
										  .withMaxTries(100)
										  .withMaxRuntime(Duration.ZERO)
										  .withFilterOutDuplicateSamples(false)
										  .withGeneration(PropertyValidationStrategy.GenerationMode.GROWING)
										  .withConcurrency(PropertyValidationStrategy.ConcurrencyMode.SINGLE_THREAD)
										  .build();


			var property = PropertyDescription.property().forAll(
				Numbers.integers().between(-100, 100),
				Numbers.integers().between(10, 20)
			).verify((i1, i2) -> {
				assertThat(i1).isBetween(-100, 100);
				assertThat(i2).isBetween(10, 20);
			});

			var result = PropertyValidator.forProperty(property).validate(strategy);
			assertThat(result.isSuccessful()).isTrue();
			assertThat(result.countTries()).isEqualTo(100);
			assertThat(result.countChecks()).isEqualTo(100);
		}

	}
}
