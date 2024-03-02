package jqwik2;

import java.time.*;
import java.util.*;
import java.util.concurrent.atomic.*;

import jqwik2.api.Arbitrary;
import jqwik2.api.Assume;
import jqwik2.api.*;
import jqwik2.api.arbitraries.*;
import jqwik2.api.database.*;
import jqwik2.api.recording.*;
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

class JqwikPropertyTests {

	@BeforeProperty
	void resetFailureDatabase() {
		JqwikDefaults.defaultFailureDatabase().clear();
	}

	@Example
	void propertyWith1ParameterSucceeds() {
		var property = new JqwikProperty();

		PropertyRunResult result = property.forAll(Numbers.integers()).check(i -> {
			assertThat(i).isInstanceOf(Integer.class);
			return true;
		});
		assertThat(result.isSuccessful()).isTrue();
		assertThat(result.countTries()).isEqualTo(100);
		assertThat(result.countChecks()).isEqualTo(100);

		result = property.forAll(Numbers.integers()).verify(i -> {
			// Thread.sleep(10);
			// System.out.println(i);
		});
		assertThat(result.isSuccessful()).isTrue();
		assertThat(result.countTries()).isEqualTo(100);
		assertThat(result.countChecks()).isEqualTo(100);
	}

	@Example
	void propertyWithAssumptionSucceeds() {
		var property = new JqwikProperty();

		PropertyRunResult result = property.forAll(Numbers.integers()).check(i -> {
			assertThat(i).isInstanceOf(Integer.class);
			return true;
		});
		assertThat(result.isSuccessful()).isTrue();
		assertThat(result.countTries()).isEqualTo(100);
		assertThat(result.countChecks()).isEqualTo(100);

		result = property.forAll(Numbers.integers()).verify(i -> {
			Assume.that(i % 2 == 0);
			// Thread.sleep(10);
			// System.out.println(i);
		});
		assertThat(result.isSuccessful()).isTrue();
		assertThat(result.countTries()).isEqualTo(100);
		// System.out.println(result.countChecks());
		assertThat(result.countChecks()).isBetween(20, 80);
	}

	@Example
	void propertyWith1ParameterFails() {
		var checkingProperty = new JqwikProperty("cp");

		PropertyRunResult checkingResult = checkingProperty.forAll(Numbers.integers()).check(i -> false);
		assertThat(checkingResult.isFailed()).isTrue();
		assertThat(checkingResult.countTries()).isEqualTo(1);
		assertThat(checkingResult.countChecks()).isEqualTo(1);

		var verifyingProperty = new JqwikProperty("vp");
		PropertyRunResult verifyingResult = verifyingProperty.forAll(Numbers.integers()).verify(i -> {
			throw new AssertionError("failed");
		});
		assertThat(verifyingResult.isFailed()).isTrue();
		assertThat(verifyingResult.countTries()).isEqualTo(1);
		assertThat(verifyingResult.countChecks()).isEqualTo(1);
	}

	@Example
	void failingPropertyThrowsExceptionWith_throwIfNotSuccessful() {
		var property = new JqwikProperty();

		assertThatThrownBy(
			() -> property.forAll(Values.just(42)).check(i -> false).throwIfNotSuccessful()
		).isInstanceOf(AssertionError.class)
		 .hasMessageContaining("Property check failed");
	}

	@Example
	void abortedPropertyThrowsExceptionWith_throwIfNotSuccessful() {
		var property = new JqwikProperty();

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

	@Example
	void propertyWith2ParametersSucceeds() {
		var strategy = PropertyRunStrategy.builder()
										  .withGeneration(PropertyRunStrategy.GenerationMode.RANDOMIZED)
										  .build();
		var property = new JqwikProperty(strategy);

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

	@Example
	void propertyWith2ParametersFails() {
		var property = new JqwikProperty();

		PropertyRunResult result = property.forAll(
			Values.just(1),
			Values.just(2)
		).verify((i1, i2) -> {
			fail("failed");
		});
		assertThat(result.isFailed()).isTrue();
	}

	@Example
	void autoPropertyId() {
		var propertyWithDefaultId = new JqwikProperty();
		String defaultId = getClass().getName() + "#" + "autoPropertyId";
		assertThat(propertyWithDefaultId.id()).isEqualTo(defaultId);

		var propertyWithExplicitId = new JqwikProperty("myId");
		assertThat(propertyWithExplicitId.id()).isEqualTo("myId");
	}

	@Example
	void propertyDefaultStrategyReporting() {
		var property = new JqwikProperty();
		PropertyRunStrategy strategy = property.strategy();

		assertThat(strategy.maxTries()).isEqualTo(100);
		assertThat(strategy.maxRuntime()).isEqualTo(Duration.ofMinutes(10));
		assertThat(strategy.shrinking()).isEqualTo(PropertyRunStrategy.ShrinkingMode.FULL);
		assertThat(strategy.generation()).isEqualTo(PropertyRunStrategy.GenerationMode.SMART);
		assertThat(strategy.edgeCases()).isEqualTo(PropertyRunStrategy.EdgeCasesMode.MIXIN);
	}

	@Example
	void edgeCasesMode_MIXIN() {
		PropertyRunStrategy strategy =
			PropertyRunStrategy.builder()
							   .withMaxTries(1000)
							   .withMaxRuntime(Duration.ofMinutes(10))
							   .withFilterOutDuplicateSamples(false)
							   .withSeedSupplier(RandomChoice::generateRandomSeed)
							   .withShrinking(PropertyRunStrategy.ShrinkingMode.OFF)
							   .withGeneration(PropertyRunStrategy.GenerationMode.RANDOMIZED)
							   .withEdgeCases(PropertyRunStrategy.EdgeCasesMode.MIXIN)
							   .withAfterFailure(PropertyRunStrategy.AfterFailureMode.SAMPLES_ONLY)
							   .withConcurrency(PropertyRunStrategy.ConcurrencyMode.SINGLE_THREAD)
							   .build();

		var property = new JqwikProperty(strategy);

		List<Integer> values = Collections.synchronizedList(new ArrayList<>());
		PropertyRunResult resultExhaustive = property.forAll(Numbers.integers()).verify(values::add);
		assertThat(resultExhaustive.isSuccessful()).isTrue();
		assertThat(resultExhaustive.countTries()).isEqualTo(1000);

		assertThat(values).contains(0, 1, -1, Integer.MAX_VALUE, Integer.MIN_VALUE);
	}

	@Example
	void failedPropertyRunWillBeSavedToFailureDatabase() {
		var property = new JqwikProperty("myId");
		var database = mock(FailureDatabase.class);
		property.failureDatabase(database);
		property.forAll(Numbers.integers()).check(i -> false);
		verify(database).saveFailure(Mockito.eq("myId"), anyString(), anySet());
	}

	@Example
	void successfulPropertyRunWillBeRemovedFromFailureDatabase() {
		var property = new JqwikProperty("myId");
		var database = mock(FailureDatabase.class);
		property.failureDatabase(database);
		property.forAll(Numbers.integers()).check(i -> true);
		verify(database).deleteProperty("myId");
	}

	@Example
	void concurrencyMode_CACHED_THREAD_POOL() {
		var strategy = PropertyRunStrategy.builder()
										  .withConcurrency(PropertyRunStrategy.ConcurrencyMode.CACHED_THREAD_POOL)
										  .withMaxTries(1000)
										  .build();
		var property = new JqwikProperty(strategy);

		var arbitrary = Numbers.integers();

		// Takes > 10 secs in single thread mode, but only 1 sec in cached thread pool mode
		PropertyRunResult initialResult = property.forAll(arbitrary).verify(i -> {
			ConcurrentRunnerTests.sleepInThread(10);
		});

		assertThat(initialResult.isSuccessful()).isTrue();
		assertThat(initialResult.countTries()).isEqualTo(1000);
	}

	@Example
	void propertyWithMaxTriesSetTo0_runsUntilTimeout() {
		var strategy = PropertyRunStrategy.builder()
										  .withMaxTries(0)
										  .withMaxRuntime(Duration.ofSeconds(1))
										  .build();
		var checkingProperty = new JqwikProperty(strategy);

		AtomicInteger counter = new AtomicInteger();
		PropertyRunResult checkingResult = checkingProperty.forAll(Numbers.integers())
														   .verify(i -> counter.incrementAndGet());

		// Should take about 1 second till maxRuntime is reached
		assertThat(checkingResult.isSuccessful()).isTrue();
		assertThat(checkingResult.countTries()).isGreaterThan(1);
		assertThat(checkingResult.countChecks()).isEqualTo(checkingResult.countTries());
	}

	@Example
	void propertyWithMaxDurationSetTo0_runsUntilMaxTriesIsReached() {
		var strategy = PropertyRunStrategy.builder()
										  .withMaxTries(10)
										  .withMaxRuntime(Duration.ZERO)
										  .build();

		var checkingProperty = new JqwikProperty(strategy);

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

	@Example
	void failedTryWillStopEndlessRunningProperty() {
		var strategy = PropertyRunStrategy.builder()
										  .withGeneration(PropertyRunStrategy.GenerationMode.RANDOMIZED)
										  .withAfterFailure(PropertyRunStrategy.AfterFailureMode.REPLAY)
										  .withMaxTries(0)
										  .withMaxRuntime(Duration.ZERO)
										  .build();
		var checkingProperty = new JqwikProperty(strategy);

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

		@Example
		void afterFailureMode_REPLAY() {
			var strategy = PropertyRunStrategy.builder()
											  .withAfterFailure(PropertyRunStrategy.AfterFailureMode.REPLAY)
											  .build();
			var property = new JqwikProperty("idFor_REPLAY", strategy);

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

		@Example
		void afterFailureMode_SAMPLES_ONLY() {
			var strategy = PropertyRunStrategy.builder()
											  .withAfterFailure(PropertyRunStrategy.AfterFailureMode.SAMPLES_ONLY)
											  .build();
			var property = new JqwikProperty("idFor_SAMPLES_ONLY", strategy);

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

		@Example
		void generationMode_EXHAUSTIVE() {
			PropertyRunStrategy strategy =
				PropertyRunStrategy.builder()
								   .withMaxTries(100)
								   .withMaxRuntime(Duration.ofMinutes(10))
								   .withGeneration(PropertyRunStrategy.GenerationMode.EXHAUSTIVE)
								   .withConcurrency(PropertyRunStrategy.ConcurrencyMode.SINGLE_THREAD)
								   .build();

			var property = new JqwikProperty(strategy);

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

		@Example
		void generationMode_SMART() {
			PropertyRunStrategy strategy =
				PropertyRunStrategy.builder()
								   .withMaxTries(100)
								   .withMaxRuntime(Duration.ofMinutes(10))
								   .withFilterOutDuplicateSamples(false)
								   .withSeedSupplier(RandomChoice::generateRandomSeed)
								   .withGeneration(PropertyRunStrategy.GenerationMode.SMART)
								   .withConcurrency(PropertyRunStrategy.ConcurrencyMode.SINGLE_THREAD)
								   .build();

			var property = new JqwikProperty(strategy);

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

			// Add sample with too many parts, which should be ignored
			sampleRecordings.add(new SampleRecording(
				Recording.tuple(42, 1),
				choice(0)
			));

			PropertyRunStrategy strategy =
				PropertyRunStrategy.builder()
								   .withMaxTries(1000)
								   .withMaxRuntime(Duration.ofMinutes(10))
								   .withFilterOutDuplicateSamples(true)
								   .withSamples(sampleRecordings)
								   .withShrinking(PropertyRunStrategy.ShrinkingMode.OFF)
								   .withGeneration(PropertyRunStrategy.GenerationMode.SAMPLES)
								   .withConcurrency(PropertyRunStrategy.ConcurrencyMode.SINGLE_THREAD)
								   .build();
			var property = new JqwikProperty(strategy);

			List<Integer> values = Collections.synchronizedList(new ArrayList<>());
			PropertyRunResult resultExhaustive = property.forAll(integers).verify(values::add);
			assertThat(resultExhaustive.countTries()).isEqualTo(sampleValues.size());
			assertThat(values).isEqualTo(sampleValues);
		}

		@Example
		void generationMode_GROWING() {
			PropertyRunStrategy strategy =
				PropertyRunStrategy.builder()
								   .withMaxTries(100)
								   .withMaxRuntime(Duration.ZERO)
								   .withFilterOutDuplicateSamples(false)
								   .withGeneration(PropertyRunStrategy.GenerationMode.GROWING)
								   .withConcurrency(PropertyRunStrategy.ConcurrencyMode.SINGLE_THREAD)
								   .build();


			var property = new JqwikProperty(strategy);

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
