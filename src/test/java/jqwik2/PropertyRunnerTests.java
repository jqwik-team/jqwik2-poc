package jqwik2;

import java.time.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;
import java.util.function.*;

import jqwik2.api.Assume;
import jqwik2.api.*;
import jqwik2.api.recording.*;
import jqwik2.api.validation.*;
import jqwik2.internal.*;
import jqwik2.internal.generators.*;
import jqwik2.internal.statistics.*;

import net.jqwik.api.Arbitrary;
import net.jqwik.api.*;

import static jqwik2.api.validation.PropertyValidationStatus.*;
import static jqwik2.internal.PropertyRunConfiguration.*;
import static org.assertj.core.api.Assertions.*;

class PropertyRunnerTests {

	@Provide
	Arbitrary<Supplier<ExecutorService>> serviceSuppliers() {
		return Arbitraries.of(
			Executors::newCachedThreadPool,
			Executors::newVirtualThreadPerTaskExecutor,
			() -> Executors.newFixedThreadPool(4),
			null // Will use InMainThreadRunner
		);
	}

	@Property(generation = GenerationMode.EXHAUSTIVE)
	void runSuccessfulProperty(@ForAll("serviceSuppliers") Supplier<ExecutorService> serviceSupplier) {
		List<Generator<?>> generators = List.of(
			new IntegerGenerator(0, 100)
		);
		Tryable tryable = Tryable.from(args -> true);

		PropertyRunner runner = new PropertyRunner(generators, tryable);

		PropertyRunResult result = runner.run(
			randomized(
				"42", 10,
				Duration.ofSeconds(10), false,
				false,
				serviceSupplier
			)
		);
		assertThat(result.status()).isEqualTo(SUCCESSFUL);
		assertThat(result.countTries()).isEqualTo(10);
		assertThat(result.countChecks()).isEqualTo(10);
	}

	@Example
	void runSuccessfulPropertyWithTwoParameters() {
		List<Generator<?>> generators = List.of(
			new IntegerGenerator(0, 100),
			new IntegerGenerator(-100, -1)
		);
		Tryable tryable = Tryable.from(args -> {
			int first = (int) args.get(0);
			int second = (int) args.get(1);
			return first > second;
		});

		PropertyRunner runner = new PropertyRunner(generators, tryable);

		PropertyRunResult result = runner.run(
			randomized("42", 10, false, false)
		);
		assertThat(result.status()).isEqualTo(PropertyValidationStatus.SUCCESSFUL);
		assertThat(result.countTries()).isEqualTo(10);
		assertThat(result.countChecks()).isEqualTo(10);
	}

	@Property(generation = GenerationMode.EXHAUSTIVE)
	void runSuccessfulWithInvalids(@ForAll("serviceSuppliers") Supplier<ExecutorService> serviceSupplier) {
		List<Generator<?>> generators = List.of(
			new IntegerGenerator(0, 100)
		);
		Tryable tryable = Tryable.from(args -> {
			int first = (int) args.get(0);
			Assume.that(first % 2 == 0);
			return true;
		});

		PropertyRunner runner = new PropertyRunner(generators, tryable);

		PropertyRunResult result = runner.run(
			randomized(
				"42", 10,
				Duration.ofSeconds(10), false,
				false,
				serviceSupplier
			)
		);
		assertThat(result.status()).isEqualTo(PropertyValidationStatus.SUCCESSFUL);
		assertThat(result.countTries()).isEqualTo(10);
		// 5 invalids - depends on random seed
		assertThat(result.countChecks()).isEqualTo(3);
	}

	@Property(generation = GenerationMode.EXHAUSTIVE)
	void failPropertyWithNoReason(@ForAll("serviceSuppliers") Supplier<ExecutorService> serviceSupplier) {
		List<Generator<?>> generators = List.of(
			new IntegerGenerator(0, 100)
		);
		int[] lastArg = new int[1];
		Tryable tryable = Tryable.from(args -> {
			lastArg[0] = (int) args.get(0);
			return false;
		});

		PropertyRunner runner = new PropertyRunner(generators, tryable);

		PropertyRunResult result = runner.run(
			randomized(
				"42", 10,
				Duration.ofSeconds(10), false,
				false,
				serviceSupplier
			)
		);
		assertThat(result.status()).isEqualTo(PropertyValidationStatus.FAILED);
		assertThat(result.countTries()).isGreaterThanOrEqualTo(1);
		assertThat(result.countChecks()).isGreaterThanOrEqualTo(1);
		// In concurrent runs checks can be fewer than tries
		assertThat(result.countChecks()).isLessThanOrEqualTo(result.countTries());
		assertThat(result.falsifiedSamples()).hasSizeGreaterThanOrEqualTo(1);
		assertThat(result.failureReason()).isEmpty();
		assertThat(result.falsifiedSamples()).anyMatch(s -> s.values().equals(List.of(lastArg[0])));
	}

	@Property(generation = GenerationMode.EXHAUSTIVE)
	void failPropertyWithReason(@ForAll("serviceSuppliers") Supplier<ExecutorService> serviceSupplier) {
		List<Generator<?>> generators = List.of(BaseGenerators.integers(0, 100));
		var assertionError = new AssertionError("I failed!");
		Tryable tryable = Tryable.from((Consumer<List<Object>>) args -> {
			throw assertionError;
		});

		PropertyRunner runner = new PropertyRunner(generators, tryable);

		PropertyRunResult result = runner.run(
			randomized(
				"42", 10,
				Duration.ofSeconds(10), true,
				false,
				serviceSupplier
			)
		);
		assertThat(result.status()).isEqualTo(PropertyValidationStatus.FAILED);
		assertThat(result.falsifiedSamples()).hasSizeGreaterThanOrEqualTo(1);
		assertThat(result.failureReason()).isPresent().hasValue(assertionError);

		FalsifiedSample smallest = result.falsifiedSamples().getFirst();
		assertThat(smallest.thrown()).isPresent().hasValue(assertionError);
	}

	@Example
	void failPropertyWithException() {
		List<Generator<?>> generators = List.of(
			new IntegerGenerator(0, 100)
		);
		int[] lastArg = new int[1];
		Tryable tryable = Tryable.from(args -> {
			lastArg[0] = (int) args.get(0);
			fail("I failed!");
		});

		PropertyRunner runner = new PropertyRunner(generators, tryable);

		PropertyRunResult result = runner.run(
			randomized("42", 10, false, false)
		);
		assertThat(result.status()).isEqualTo(PropertyValidationStatus.FAILED);
		FalsifiedSample smallest = result.falsifiedSamples().getFirst();
		assertThat(smallest.values())
			.isEqualTo(List.of(lastArg[0]));
		assertThat(smallest.thrown()).isPresent();
		smallest.thrown().ifPresent(throwable -> {
			assertThat(throwable).isInstanceOf(AssertionError.class);
			assertThat(throwable).hasMessage("I failed!");
		});
	}

	@Property(generation = GenerationMode.EXHAUSTIVE)
	void failAndShrink(@ForAll("serviceSuppliers") Supplier<ExecutorService> serviceSupplier) {
		List<Generator<?>> generators = List.of(
			new IntegerGenerator(0, 100)
		);
		Tryable tryable = Tryable.from(args -> {
			int anInt = (int) args.get(0);
			return anInt < 45;
		});

		PropertyRunner runner = new PropertyRunner(generators, tryable);

		PropertyRunResult result = runner.run(
			randomized(
				"4242", 100,
				Duration.ofSeconds(10), true,
				false,
				serviceSupplier
			)
		);
		assertThat(result.status()).isEqualTo(PropertyValidationStatus.FAILED);
		assertThat(result.countTries()).isGreaterThan(1);
		assertThat(result.falsifiedSamples()).hasSizeGreaterThan(1);
		FalsifiedSample smallest = result.falsifiedSamples().getFirst();
		assertThat(smallest.values()).isEqualTo(List.of(45));
		FalsifiedSample biggest = result.falsifiedSamples().getLast();
		assertThat(biggest).isGreaterThan(smallest);
	}

	@Property(generation = GenerationMode.EXHAUSTIVE)
	void runSuccessfulWithMaxDuration(@ForAll("serviceSuppliers") Supplier<ExecutorService> serviceSupplier) {
		List<Generator<?>> generators = List.of(
			new IntegerGenerator(0, 100)
		);

		AtomicBoolean firstHasRun = new AtomicBoolean(false);
		Tryable tryable = Tryable.from(args -> {
			if (firstHasRun.get()) {
				try {
					Thread.sleep(2000);
				} catch (InterruptedException e) {
				}
			} else {
				firstHasRun.set(true);
			}
			return true;
		});

		PropertyRunner runner = new PropertyRunner(generators, tryable);

		PropertyRunResult result = runner.run(
			randomized(
				"42", 1000, Duration.ofSeconds(1), false,
				false,
				serviceSupplier
			)
		);
		assertThat(result.status()).isEqualTo(PropertyValidationStatus.SUCCESSFUL);
		assertThat(result.timedOut()).isTrue();
		assertThat(result.countTries()).isGreaterThan(0);
		assertThat(result.countChecks()).isLessThanOrEqualTo(result.countTries());
	}

	@Property(generation = GenerationMode.EXHAUSTIVE)
	void failWithTimeout(@ForAll("serviceSuppliers") Supplier<ExecutorService> serviceSupplier) {

		Tryable tryable = Tryable.from(args -> {
			try {
				Thread.sleep(5000);
			} catch (InterruptedException e) {
				fail("I was interrupted!");
			}
		});

		Generator<?> anyGenerator = new IntegerGenerator(0, 100);
		PropertyRunner runner = new PropertyRunner(List.of(anyGenerator), tryable);

		PropertyRunResult result = runner.run(
			randomized(
				"42", 100, Duration.ofMillis(200), false,
				false,
				serviceSupplier
			)
		);
		assertThat(result.status()).isIn(PropertyValidationStatus.FAILED, PropertyValidationStatus.ABORTED);
	}

	@Property(generation = GenerationMode.EXHAUSTIVE)
	void abort(@ForAll("serviceSuppliers") Supplier<ExecutorService> serviceSupplier) {
		Tryable tryable = Tryable.from(args -> {
			try {
				Thread.sleep(5000);
			} catch (InterruptedException e) {
				throw new RuntimeException(e);
			}
		});

		var abortion = new RuntimeException("I fail!");
		Generator<?> aFailingGenerator = (Generator<Object>) source -> {
			throw abortion;
		};

		PropertyRunner runner = new PropertyRunner(List.of(aFailingGenerator), tryable);

		String seed = RandomChoice.generateRandomSeed();
		PropertyRunResult result = runner.run(
			randomized(
				seed, 100, Duration.ofMinutes(10), true,
				false,
				serviceSupplier
			)
		);
		assertThat(result.status()).isEqualTo(PropertyValidationStatus.ABORTED);
		assertThat(result.abortionReason()).hasValue(abortion);
	}

	@Property(tries = 10)
	void reproduceSameSamplesWithVirtualTasks(@ForAll long seed) {
		reproduceSameSamplesTwice(
			seed,
			Executors::newVirtualThreadPerTaskExecutor
		);
	}

	@Property(tries = 10)
	void reproduceSameSamplesWithFixedSizeCachedPool(@ForAll long seed) {
		reproduceSameSamplesTwice(
			seed,
			() -> Executors.newFixedThreadPool(4)
		);
	}

	@Property(tries = 10)
	void reproduceSameSamplesWithCachedThreadPool(@ForAll long seed) {
		reproduceSameSamplesTwice(
			seed,
			Executors::newCachedThreadPool
		);
	}

	@Property(tries = 10)
	void reproduceSameSamplesInMainThreadRunner(@ForAll long seed) {
		reproduceSameSamplesTwice(
			seed,
			null
		);
	}

	private static void reproduceSameSamplesTwice(long seed, Supplier<ExecutorService> serviceSupplier) {
		List<Generator<?>> generators = List.of(
			new IntegerGenerator(0, 100)
		);
		Tryable tryable = Tryable.from(args -> true);

		PropertyRunner runner = new PropertyRunner(generators, tryable);

		final Set<Sample> collectedSamples = Collections.synchronizedSet(new HashSet<>());
		runner.registerTryExecutionListener((result, sample) -> collectedSamples.add(sample));

		PropertyRunConfiguration runConfiguration = randomized(
			Long.toString(seed), 10, Duration.ofSeconds(10), false,
			false,
			serviceSupplier
		);

		assertThat(runner.run(runConfiguration).status())
			.isEqualTo(PropertyValidationStatus.SUCCESSFUL);

		Set<Sample> samplesFirstRun = new HashSet<>(collectedSamples);
		collectedSamples.clear();

		assertThat(runner.run(runConfiguration).status())
			.isEqualTo(PropertyValidationStatus.SUCCESSFUL);

		assertThat(samplesFirstRun).hasSameElementsAs(collectedSamples);
	}

	@Group
	class OnTryExecutionHandlers {

		@Example
		void allTryExecutionsAreAnnounced() {
			List<Generator<?>> generators = List.of(
				new IntegerGenerator(1, 100)
			);
			Tryable tryable = Tryable.from(args -> {
				int first = (int) args.get(0);
				Assume.that(first % 2 == 0);
				return first < 50;
			});

			PropertyRunner runner = new PropertyRunner(generators, tryable);

			AtomicInteger countSatisfied = new AtomicInteger(0);
			AtomicInteger countInvalid = new AtomicInteger(0);
			AtomicInteger countFalsified = new AtomicInteger(0);
			runner.registerTryExecutionListener((result, sample) -> {
				switch(result.status()) {
					case SATISFIED:
						countSatisfied.incrementAndGet();
						break;
					case INVALID:
						countInvalid.incrementAndGet();
						break;
					case FALSIFIED:
						countFalsified.incrementAndGet();
						break;
				}
			});

			PropertyRunResult result = runner.run(
				exhaustive(
					1000, Duration.ofSeconds(10),
					null, generators
				)
			);
			assertThat(result.isFailed()).isTrue();
			assertThat(result.countTries()).isEqualTo(50);
			assertThat(result.countChecks()).isEqualTo(25);

			assertThat(countSatisfied.get()).isEqualTo(24);
			assertThat(countFalsified.get()).isEqualTo(1);
			assertThat(countInvalid.get()).isEqualTo(25);
		}

		@Example
		void tryExecutionsDuringShrinkingAreAnnounced() {
			List<Generator<?>> generators = List.of(
				new IntegerGenerator(0, 100)
			);
			Tryable tryable = Tryable.from(args -> {
				int first = (int) args.get(0);
				Assume.that(first % 2 == 0);
				return first < 50;
			});

			PropertyRunner runner = new PropertyRunner(generators, tryable);

			AtomicInteger countSatisfied = new AtomicInteger(0);
			AtomicInteger countInvalid = new AtomicInteger(0);
			AtomicInteger countFalsified = new AtomicInteger(0);
			runner.registerTryExecutionListener((result, sample) -> {
				switch(result.status()) {
					case SATISFIED:
						countSatisfied.incrementAndGet();
						break;
					case INVALID:
						countInvalid.incrementAndGet();
						break;
					case FALSIFIED:
						countFalsified.incrementAndGet();
						break;
				}
			});

			List<SampleRecording> samples = List.of(new SampleRecording(Recording.choice(80)));

			PropertyRunResult result = runner.run(
				samples(
					 Duration.ofSeconds(10), true,
					samples, null
				)
			);
			assertThat(result.status()).isEqualTo(PropertyValidationStatus.FAILED);
			assertThat(result.falsifiedSamples().getFirst().values()).isEqualTo(List.of(50));

			assertThat(countFalsified.get()).isGreaterThan(1);
			assertThat(countSatisfied.get()).isGreaterThan(0);
			assertThat(countInvalid.get()).isGreaterThan(0);
		}

	}

	@Group
	class SequentialRuns {

		@Property(generation = GenerationMode.EXHAUSTIVE)
		void threeSuccessfulRuns(@ForAll("serviceSuppliers") Supplier<ExecutorService> serviceSupplier) {
			List<Generator<?>> generators = List.of(
				new IntegerGenerator(1, 100)
			);
			Tryable tryable = Tryable.from(args -> true);

			PropertyRunner runner = new PropertyRunner(generators, tryable);

			var run1 = randomized(
				"42", 10,
				Duration.ofSeconds(10), false,
				false,
				serviceSupplier
			);

			var run2 = exhaustive(serviceSupplier, generators);
			List<PropertyRunResult> results = runner.run(List.of(run1, run2, run1));

			assertThat(results).hasSize(3);
			assertThat(results.stream().map(PropertyRunResult::status)).containsExactly(SUCCESSFUL, SUCCESSFUL, SUCCESSFUL);
			assertThat(results.stream().map(PropertyRunResult::countTries)).containsExactly(10, 100, 10);
		}

		@Property(generation = GenerationMode.EXHAUSTIVE)
		void secondOfThreeRunsFails(@ForAll("serviceSuppliers") Supplier<ExecutorService> serviceSupplier) {
			List<Generator<?>> generators = List.of(
				new IntegerGenerator(1, 100)
			);
			Tryable tryable = Tryable.from(args -> {
				int anInt = (int) args.get(0);
				return anInt < 42;
			});

			PropertyRunner runner = new PropertyRunner(generators, tryable);

			List<SampleRecording> samples = List.of(
				new SampleRecording(Recording.choice(10)),
				new SampleRecording(Recording.choice(13)),
				new SampleRecording(Recording.choice(27))
			);
			var run1 = samples(samples, serviceSupplier);

			var run2 = exhaustive(serviceSupplier, generators);

			List<PropertyRunResult> results = runner.run(List.of(run1, run2, run1));

			assertThat(results).hasSize(2);
			assertThat(results.stream().map(PropertyRunResult::status)).containsExactly(SUCCESSFUL, FAILED);

			var failed = results.get(1);
			assertThat(failed.countChecks()).isGreaterThanOrEqualTo(42);
			assertThat(failed.falsifiedSamples().getFirst().values()).isEqualTo(List.of(42));
		}

	}

	@Group
	class StatisticalProperties {

		@Property(generation = GenerationMode.EXHAUSTIVE)
		void succeedWithMinPercentage(@ForAll("serviceSuppliers") Supplier<ExecutorService> serviceSupplier) {

			List<Generator<?>> generators = List.of(
				BaseGenerators.integers(0, 100)
			);

			Tryable tryable = Tryable.from(args -> {
				int anInt = (int) args.get(0);
				return anInt < 95;
			});

			PropertyRunner runner = new PropertyRunner(generators, tryable);

			PropertyRunResult result = runner.run(
				wrapSource(
					randomized(
						"42", 0, Duration.ZERO,
						false, false,
						serviceSupplier
					),
					source -> new StatisticalPropertySource(source, 93.0, 2.0)
				)
			);
			// System.out.println(result.countChecks());
			assertThat(result.status()).isEqualTo(PropertyValidationStatus.SUCCESSFUL);
		}

		@Property(generation = GenerationMode.EXHAUSTIVE)
		void failWithMinPercentage(@ForAll("serviceSuppliers") Supplier<ExecutorService> serviceSupplier) {

			List<Generator<?>> generators = List.of(
				BaseGenerators.integers(0, 100)
			);

			Tryable tryable = Tryable.from(args -> {
				int anInt = (int) args.get(0);
				return anInt < 88;
			});

			PropertyRunner runner = new PropertyRunner(generators, tryable);

			PropertyRunResult result = runner.run(
				wrapSource(
					randomized(
						"42", 0, Duration.ZERO,
						false, false,
						serviceSupplier
					),
					source -> new StatisticalPropertySource(source, 90.0, 2.0)
				)
			);
			// System.out.println(result.countChecks());
			// System.out.println(result.failureReason().get().getMessage());
			assertThat(result.status()).isEqualTo(PropertyValidationStatus.FAILED);
		}

	}

	@Group
	class StatisticallyGuidedGeneration {

		@Property(generation = GenerationMode.EXHAUSTIVE)
		void succeedStatisticallyGuided(@ForAll("serviceSuppliers") Supplier<ExecutorService> serviceSupplier) {

			List<Generator<?>> generators = List.of(
				BaseGenerators.integers(0, 100)
			);

			var classifier = new ClassifyingCollector<List<Object>>();
			classifier.addCase("Even", 40.0, args -> (int) args.get(0) % 2 == 0);
			classifier.addCase("Odd", 40.0, args -> (int) args.get(0) % 2 != 0);

			Tryable tryable = Tryable.from(classifier::classify);

			PropertyRunner runner = new PropertyRunner(generators, tryable);

			PropertyRunResult result = runner.run(
				wrapSource(
					randomized(
						"42", 0, Duration.ZERO,
						false, false,
						serviceSupplier
					),
					source -> new StatisticallyGuidedGenerationSource(source, Set.of(classifier), 2.0)
				)
			);
			assertThat(result.status()).isEqualTo(PropertyValidationStatus.SUCCESSFUL);

			// System.out.println(classifier.total());
			// System.out.println(classifier.percentages());
		}

		@Property(generation = GenerationMode.EXHAUSTIVE)
		void failStatisticallyGuided(@ForAll("serviceSuppliers") Supplier<ExecutorService> serviceSupplier) {

			List<Generator<?>> generators = List.of(
				BaseGenerators.integers(0, 100)
			);

			var classifier = new ClassifyingCollector<List<Object>>();
			classifier.addCase("Even", 55.0, args -> (int) args.get(0) % 2 == 0);
			classifier.addCase("Odd", 40.0, args -> (int) args.get(0) % 2 != 0);

			Tryable tryable = Tryable.from(classifier::classify);

			PropertyRunner runner = new PropertyRunner(generators, tryable);

			PropertyRunResult result = runner.run(
				wrapSource(
					randomized(
						"42", 0, Duration.ZERO,
						false, false,
						serviceSupplier
					),
					source -> new StatisticallyGuidedGenerationSource(source, Set.of(classifier), 2.0)
				)
			);
			assertThat(result.status()).isEqualTo(PropertyValidationStatus.FAILED);
			assertThat(result.failureReason()).isPresent();
			result.failureReason().ifPresent(throwable -> {
				assertThat(throwable.getMessage())
					.startsWith("Coverage of case 'Even' expected to be at least 55.00%");
			});

			// System.out.println(result.failureReason().get().getMessage());
			// System.out.println(classifier.total());
			// System.out.println(classifier.percentages());
			// System.out.println(classifier.rejections());
		}

		@Property(generation = GenerationMode.EXHAUSTIVE)
		void failStatisticalGuidedWithNormalFailure(@ForAll("serviceSuppliers") Supplier<ExecutorService> serviceSupplier) {

			List<Generator<?>> generators = List.of(
				BaseGenerators.integers(0, 100)
			);

			var classifier = new ClassifyingCollector<List<Object>>();
			classifier.addCase("Even", 45.0, args -> (int) args.get(0) % 2 == 0);
			classifier.addCase("Odd", 45.0, args -> (int) args.get(0) % 2 != 0);

			Tryable tryable = Tryable.from(args -> {
				classifier.classify(args);
				return classifier.total() < 100;
			});

			PropertyRunner runner = new PropertyRunner(generators, tryable);

			PropertyRunResult result = runner.run(
				wrapSource(
					randomized(
						"42", 0, Duration.ZERO,
						false, false,
						serviceSupplier
					),
					source -> new StatisticallyGuidedGenerationSource(source, Set.of(classifier), 2.0)
				)
			);
			assertThat(result.status()).isEqualTo(PropertyValidationStatus.FAILED);
			assertThat(result.failureReason()).isEmpty();

			// countChecks should be relatively close to 100, but parallel execution can make it higher
			// System.out.println(result.countChecks());
		}

		@Property(generation = GenerationMode.EXHAUSTIVE)
		void stopStatisticalGuidedAfterMaxTries(@ForAll("serviceSuppliers") Supplier<ExecutorService> serviceSupplier) {

			List<Generator<?>> generators = List.of(
				BaseGenerators.integers(0, 100)
			);

			var classifier = new ClassifyingCollector<List<Object>>();
			classifier.addCase("Even", 20.0, args -> (int) args.get(0) % 2 == 0);
			classifier.addCase("Odd", 20.0, args -> (int) args.get(0) % 2 != 0);

			Tryable tryable = Tryable.from(args -> {
				classifier.classify(args);
				return classifier.total() < 100;
			});

			PropertyRunner runner = new PropertyRunner(generators, tryable);

			PropertyRunResult result = runner.run(
				wrapSource(
					randomized(
						"42", 100, Duration.ZERO,
						false, false,
						serviceSupplier
					),
					source -> new StatisticallyGuidedGenerationSource(source, Set.of(classifier), 2.0)
				)
			);
			assertThat(result.status()).isEqualTo(PropertyValidationStatus.FAILED);
			assertThat(result.countChecks()).isEqualTo(100);
		}

		@Property(generation = GenerationMode.EXHAUSTIVE)
		void statisticalGuidedPropertiesCanAlsoBeShrunk(@ForAll("serviceSuppliers") Supplier<ExecutorService> serviceSupplier) {

			List<Generator<?>> generators = List.of(
				BaseGenerators.integers(0, 100)
			);

			var classifier = new ClassifyingCollector<List<Object>>();
			classifier.addCase("Even", 40.0, args -> (int) args.get(0) % 2 == 0);
			classifier.addCase("Odd", 40.0, args -> (int) args.get(0) % 2 != 0);

			Tryable tryable = Tryable.from(args -> {
				classifier.classify(args);
				int anInt = (int) args.get(0);
				return anInt < 30;
			});

			PropertyRunner runner = new PropertyRunner(generators, tryable);

			PropertyRunResult result = runner.run(
				wrapSource(
					randomized(
						"42", 0, Duration.ZERO,
						true, false,
						serviceSupplier
					),
					source -> new StatisticallyGuidedGenerationSource(source, Set.of(classifier), 2.0)
				)
			);

			assertThat(result.status()).isEqualTo(PropertyValidationStatus.FAILED);
			assertThat(result.falsifiedSamples().getFirst().values()).isEqualTo(List.of(30));
		}

	}

}
