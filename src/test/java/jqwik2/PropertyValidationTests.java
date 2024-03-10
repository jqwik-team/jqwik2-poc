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

import static jqwik2.api.recording.Recording.list;
import static jqwik2.api.recording.Recording.*;
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

	// @Example
	void failingPropertyThrowsExceptionWith_throwIfNotSuccessful() {
		var property = new OLD_JqwikProperty();

		assertThatThrownBy(
			() -> property.forAll(Values.just(42)).check(i -> false).throwIfNotSuccessful()
		).isInstanceOf(AssertionError.class)
		 .hasMessageContaining("Property check failed");
	}

	// @Example
	void abortedPropertyThrowsExceptionWith_throwIfNotSuccessful() {
		var property = new OLD_JqwikProperty();

		var throwingArbitrary = new Arbitrary<Integer>() {
			@Override
			public Generator<Integer> generator() {
				return source -> {
					throw new TestAbortedException("Property aborted because of thrown exception");
				};
			}
		};
		assertThatThrownBy(
			() -> property.forAll(throwingArbitrary).verify(i -> {}).throwIfNotSuccessful()
		).isInstanceOf(TestAbortedException.class)
		 .hasMessageContaining("Property aborted because of thrown exception");
	}

	// @Example
	void propertyWith2ParametersSucceeds() {
		var strategy = PropertyValidationStrategy.builder()
												 .withGeneration(PropertyValidationStrategy.GenerationMode.RANDOMIZED)
												 .build();
		var property = new OLD_JqwikProperty(strategy);

		PropertyRunResult result = property.forAll(
			Values.just(1),
			Values.just(2)
		).verify((i1, i2) -> {
			assertThat(i1).isEqualTo(1);
			assertThat(i2).isEqualTo(2);
		});
		assertThat(result.isSuccessful()).isTrue();
		assertThat(result.countTries()).isEqualTo(100);
		assertThat(result.countChecks()).isEqualTo(100);
	}

	// @Example
	void propertyWith2ParametersFails() {
		var property = new OLD_JqwikProperty();

		PropertyRunResult result = property.forAll(
			Values.just(1),
			Values.just(2)
		).verify((i1, i2) -> {
			fail("failed");
		});
		assertThat(result.isFailed()).isTrue();
	}

	// @Example
	void autoPropertyId() {
		var propertyWithDefaultId = new OLD_JqwikProperty();
		String defaultId = getClass().getName() + "#" + "autoPropertyId";
		assertThat(propertyWithDefaultId.id()).isEqualTo(defaultId);

		var propertyWithExplicitId = new OLD_JqwikProperty("myId");
		assertThat(propertyWithExplicitId.id()).isEqualTo("myId");
	}

	// @Example
	void propertyDefaultStrategyReporting() {
		var property = new OLD_JqwikProperty();
		PropertyValidationStrategy strategy = property.strategy();

		assertThat(strategy.maxTries()).isEqualTo(100);
		assertThat(strategy.maxRuntime()).isEqualTo(Duration.ofMinutes(10));
		assertThat(strategy.shrinking()).isEqualTo(PropertyValidationStrategy.ShrinkingMode.FULL);
		assertThat(strategy.generation()).isEqualTo(PropertyValidationStrategy.GenerationMode.SMART);
		assertThat(strategy.edgeCases()).isEqualTo(PropertyValidationStrategy.EdgeCasesMode.MIXIN);
	}

	// @Example
	void edgeCasesMode_MIXIN() {
		PropertyValidationStrategy strategy =
			PropertyValidationStrategy.builder()
									  .withMaxTries(1000)
									  .withMaxRuntime(Duration.ofMinutes(10))
									  .withFilterOutDuplicateSamples(false)
									  .withSeedSupplier(RandomChoice::generateRandomSeed)
									  .withShrinking(PropertyValidationStrategy.ShrinkingMode.OFF)
									  .withGeneration(PropertyValidationStrategy.GenerationMode.RANDOMIZED)
									  .withEdgeCases(PropertyValidationStrategy.EdgeCasesMode.MIXIN)
									  .withAfterFailure(PropertyValidationStrategy.AfterFailureMode.SAMPLES_ONLY)
									  .withConcurrency(PropertyValidationStrategy.ConcurrencyMode.SINGLE_THREAD)
									  .build();

		var property = new OLD_JqwikProperty(strategy);

		List<Integer> values = Collections.synchronizedList(new ArrayList<>());
		PropertyRunResult resultExhaustive = property.forAll(Numbers.integers()).verify(values::add);
		assertThat(resultExhaustive.isSuccessful()).isTrue();
		assertThat(resultExhaustive.countTries()).isEqualTo(1000);

		assertThat(values).contains(0, 1, -1, Integer.MAX_VALUE, Integer.MIN_VALUE);
	}

	// @Example
	void failedPropertyRunWillBeSavedToFailureDatabase() {
		var property = new OLD_JqwikProperty("myId");
		var database = mock(FailureDatabase.class);
		property.failureDatabase(database);
		property.forAll(Numbers.integers()).check(i -> false);
		verify(database).saveFailure(Mockito.eq("myId"), anyString(), anySet());
	}

	// @Example
	void successfulPropertyRunWillBeRemovedFromFailureDatabase() {
		var property = new OLD_JqwikProperty("myId");
		var database = mock(FailureDatabase.class);
		property.failureDatabase(database);
		property.forAll(Numbers.integers()).check(i -> true);
		verify(database).deleteProperty("myId");
	}

	// @Example
	void concurrencyMode_CACHED_THREAD_POOL() {
		var strategy = PropertyValidationStrategy.builder()
												 .withConcurrency(PropertyValidationStrategy.ConcurrencyMode.CACHED_THREAD_POOL)
												 .withMaxTries(1000)
												 .build();
		var property = new OLD_JqwikProperty(strategy);

		var arbitrary = Numbers.integers();

		// Takes > 10 secs in single thread mode, but only 1 sec in cached thread pool mode
		PropertyRunResult initialResult = property.forAll(arbitrary).verify(i -> {
			ConcurrentRunnerTests.sleepInThread(10);
		});

		assertThat(initialResult.isSuccessful()).isTrue();
		assertThat(initialResult.countTries()).isEqualTo(1000);
	}

	// @Example
	void propertyWithMaxTriesSetTo0_runsUntilTimeout() {
		var strategy = PropertyValidationStrategy.builder()
												 .withMaxTries(0)
												 .withMaxRuntime(Duration.ofSeconds(1))
												 .build();
		var checkingProperty = new OLD_JqwikProperty(strategy);

		AtomicInteger counter = new AtomicInteger();
		PropertyRunResult checkingResult = checkingProperty.forAll(Numbers.integers())
														   .verify(i -> counter.incrementAndGet());

		// Should take about 1 second till maxRuntime is reached
		assertThat(checkingResult.isSuccessful()).isTrue();
		assertThat(checkingResult.countTries()).isGreaterThan(1);
		assertThat(checkingResult.countChecks()).isEqualTo(checkingResult.countTries());
	}

	// @Example
	void propertyWithMaxDurationSetTo0_runsUntilMaxTriesIsReached() {
		var strategy = PropertyValidationStrategy.builder()
												 .withMaxTries(10)
												 .withMaxRuntime(Duration.ZERO)
												 .build();

		var checkingProperty = new OLD_JqwikProperty(strategy);

		AtomicInteger counter = new AtomicInteger();
		PropertyRunResult checkingResult = checkingProperty.forAll(Numbers.integers())
														   .verify(i -> {
															   Thread.sleep(100);
															   counter.incrementAndGet();
														   });

		// Should take about 1 second (10 * 100ms)
		assertThat(checkingResult.isSuccessful()).isTrue();
		assertThat(checkingResult.countTries()).isEqualTo(10);
		assertThat(checkingResult.countChecks()).isEqualTo(checkingResult.countTries());
	}

	// @Example
	void failedTryWillStopEndlessRunningProperty() {
		var strategy = PropertyValidationStrategy.builder()
												 .withGeneration(PropertyValidationStrategy.GenerationMode.RANDOMIZED)
												 .withAfterFailure(PropertyValidationStrategy.AfterFailureMode.REPLAY)
												 .withMaxTries(0)
												 .withMaxRuntime(Duration.ZERO)
												 .build();
		var checkingProperty = new OLD_JqwikProperty(strategy);

		AtomicInteger counter = new AtomicInteger();
		PropertyRunResult checkingResult = checkingProperty.forAll(Numbers.integers())
														   .verify(i -> {
															   // System.out.println(counter.get());
															   counter.incrementAndGet();
															   try {
																   Thread.sleep(10);
															   } catch (InterruptedException ignore) {
																   // Within a test run the thread can sometimes be interrupted
															   }
															   assertThat(counter.get()).isLessThan(100);
														   });

		assertThat(checkingResult.isFailed()).isTrue();
		assertThat(checkingResult.countTries()).isEqualTo(100);
		assertThat(checkingResult.countChecks()).isEqualTo(checkingResult.countTries());
	}

	@Group
	class AfterFailureModes {

		// @Example
		void afterFailureMode_REPLAY() {
			var strategy = PropertyValidationStrategy.builder()
													 .withAfterFailure(PropertyValidationStrategy.AfterFailureMode.REPLAY)
													 .build();
			var property = new OLD_JqwikProperty("idFor_REPLAY", strategy);

			var integers = Numbers.integers().between(-1000, 1000);

			List<Integer> initiallyTriedValues = new ArrayList<>();
			PropertyRunResult initialResult = property.forAll(integers).check(i -> {
				initiallyTriedValues.add(i);
				return i > -10 && i < 10;
			});
			assertThat(initialResult.isFailed()).isTrue();
			assertThat(initialResult.countTries()).isGreaterThan(0);

			List<Integer> replayedTriedValues = new ArrayList<>();
			PropertyRunResult replayedResult = property.forAll(integers).check(i -> {
				replayedTriedValues.add(i);
				return i > -10 && i < 10;
			});
			assertThat(replayedResult).isEqualTo(initialResult);
			assertThat(replayedTriedValues).isEqualTo(initiallyTriedValues);
		}

		// @Example
		void afterFailureMode_SAMPLES_ONLY() {
			var strategy = PropertyValidationStrategy.builder()
													 .withAfterFailure(PropertyValidationStrategy.AfterFailureMode.SAMPLES_ONLY)
													 .build();
			var property = new OLD_JqwikProperty("idFor_SAMPLES_ONLY", strategy);

			var integers = Numbers.integers().between(-1000, 1000);

			PropertyRunResult initialResult = property.forAll(integers).check(i -> {
				return i > -10 && i < 10;
			});
			assertThat(initialResult.isFailed()).isTrue();
			List<Integer> falsifiedValues = initialResult.falsifiedSamples().stream()
														 .map(s -> (Integer) s.sample().values().get(0))
														 .toList();
			assertThat(falsifiedValues).hasSize(2);

			List<Integer> samplesOnlyValues = new ArrayList<>();
			PropertyRunResult replayedResult = property.forAll(integers).check(i -> {
				samplesOnlyValues.add(i);
				return true; // In order to try all previously falsified samples
			});
			assertThat(replayedResult.countTries()).isEqualTo(2);
			assertThat(samplesOnlyValues).isEqualTo(falsifiedValues);
		}
	}

	@Group
	class GenerationModes {

		// @Example
		void generationMode_EXHAUSTIVE() {
			PropertyValidationStrategy strategy =
				PropertyValidationStrategy.builder()
										  .withMaxTries(100)
										  .withMaxRuntime(Duration.ofMinutes(10))
										  .withGeneration(PropertyValidationStrategy.GenerationMode.EXHAUSTIVE)
										  .withConcurrency(PropertyValidationStrategy.ConcurrencyMode.SINGLE_THREAD)
										  .build();

			var property = new OLD_JqwikProperty(strategy);

			PropertyRunResult result = property.forAll(
				Numbers.integers().between(0, 3),
				Numbers.integers().between(1, 2)
			).verify((i1, i2) -> {
				// System.out.println(i1 + " " + i2);
				assertThat(i1).isBetween(0, 3);
				assertThat(i2).isBetween(1, 2);
			});

			assertThat(result.isSuccessful()).isTrue();
			assertThat(result.countTries()).isEqualTo(8);
			assertThat(result.countChecks()).isEqualTo(8);
		}

		// @Example
		void generationMode_SMART() {
			PropertyValidationStrategy strategy =
				PropertyValidationStrategy.builder()
										  .withMaxTries(100)
										  .withMaxRuntime(Duration.ofMinutes(10))
										  .withFilterOutDuplicateSamples(false)
										  .withSeedSupplier(RandomChoice::generateRandomSeed)
										  .withGeneration(PropertyValidationStrategy.GenerationMode.SMART)
										  .withConcurrency(PropertyValidationStrategy.ConcurrencyMode.SINGLE_THREAD)
										  .build();

			var property = new OLD_JqwikProperty(strategy);

			PropertyRunResult resultExhaustive = property.forAll(
				Numbers.integers().between(0, 3),
				Numbers.integers().between(1, 2)
			).verify((i1, i2) -> {});
			assertThat(resultExhaustive.isSuccessful()).isTrue();
			assertThat(resultExhaustive.countTries()).isEqualTo(8);

			// 100 * 2 > 100 => randomized generation
			PropertyRunResult resultRandomized = property.forAll(
				Numbers.integers().between(1, 100),
				Numbers.integers().between(0, 1)
			).verify((i1, i2) -> {});
			assertThat(resultRandomized.isSuccessful()).isTrue();
			assertThat(resultRandomized.countTries()).isEqualTo(100);
		}

		// @Example
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

			// Add sample with too many parts, which should be ignored
			sampleRecordings.add(new SampleRecording(
				Recording.tuple(42, 1),
				choice(0)
			));

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
			var property = new OLD_JqwikProperty(strategy);

			List<Integer> values = Collections.synchronizedList(new ArrayList<>());
			PropertyRunResult resultExhaustive = property.forAll(integers).verify(values::add);
			assertThat(resultExhaustive.countTries()).isEqualTo(sampleValues.size());
			assertThat(values).isEqualTo(sampleValues);
		}

		// @Example
		void generationMode_GROWING() {
			PropertyValidationStrategy strategy =
				PropertyValidationStrategy.builder()
										  .withMaxTries(100)
										  .withMaxRuntime(Duration.ZERO)
										  .withFilterOutDuplicateSamples(false)
										  .withGeneration(PropertyValidationStrategy.GenerationMode.GROWING)
										  .withConcurrency(PropertyValidationStrategy.ConcurrencyMode.SINGLE_THREAD)
										  .build();


			var property = new OLD_JqwikProperty(strategy);

			PropertyRunResult result = property.forAll(
				Numbers.integers().between(-100, 100),
				Numbers.integers().between(10, 20)
			).verify((i1, i2) -> {
				// System.out.println(i1 + " " + i2);
				assertThat(i1).isBetween(-100, 100);
				assertThat(i2).isBetween(10, 20);
			});

			assertThat(result.isSuccessful()).isTrue();
			assertThat(result.countTries()).isEqualTo(100);
			assertThat(result.countChecks()).isEqualTo(100);
		}

	}
}
