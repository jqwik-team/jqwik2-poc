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

class PropertyRunTests {

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

		PropertyRun propertyRun = new PropertyRun(generators, tryable);

		PropertyRunResult result = propertyRun.run(
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

		PropertyRun propertyRun = new PropertyRun(generators, tryable);

		PropertyRunResult result = propertyRun.run(
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

		PropertyRun propertyRun = new PropertyRun(generators, tryable);

		PropertyRunResult result = propertyRun.run(
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

		PropertyRun propertyRun = new PropertyRun(generators, tryable);

		PropertyRunResult result = propertyRun.run(
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

		PropertyRun propertyRun = new PropertyRun(generators, tryable);

		PropertyRunResult result = propertyRun.run(
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

		PropertyRun propertyRun = new PropertyRun(generators, tryable);

		PropertyRunResult result = propertyRun.run(
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

		PropertyRun propertyRun = new PropertyRun(generators, tryable);

		PropertyRunResult result = propertyRun.run(
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

		PropertyRun propertyRun = new PropertyRun(generators, tryable);

		PropertyRunResult result = propertyRun.run(
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
		PropertyRun propertyRun = new PropertyRun(List.of(anyGenerator), tryable);

		PropertyRunResult result = propertyRun.run(
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

		PropertyRun propertyRun = new PropertyRun(List.of(aFailingGenerator), tryable);

		String seed = RandomChoice.generateRandomSeed();
		PropertyRunResult result = propertyRun.run(
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

		PropertyRun propertyRun = new PropertyRun(generators, tryable);

		final Set<Sample> samples1 = Collections.synchronizedSet(new HashSet<>());
		propertyRun.onSuccessful(samples1::add);

		PropertyRunConfiguration runConfiguration = randomized(
			Long.toString(seed), 10, Duration.ofSeconds(10), false,
			false,
			serviceSupplier
		);

		assertThat(propertyRun.run(runConfiguration).status())
			.isEqualTo(PropertyValidationStatus.SUCCESSFUL);

		final Set<Sample> samples2 = Collections.synchronizedSet(new HashSet<>());
		propertyRun.onSuccessful(samples2::add);
		assertThat(propertyRun.run(runConfiguration).status())
			.isEqualTo(PropertyValidationStatus.SUCCESSFUL);

		assertThat(samples1).hasSameElementsAs(samples2);
	}

	@Group
	class SequentialRuns {


		@Property(generation = GenerationMode.EXHAUSTIVE)
		void threeSuccessfulRuns(@ForAll("serviceSuppliers") Supplier<ExecutorService> serviceSupplier) {
			List<Generator<?>> generators = List.of(
				new IntegerGenerator(1, 100)
			);
			Tryable tryable = Tryable.from(args -> true);

			PropertyRun propertyRun = new PropertyRun(generators, tryable);

			var run1 = randomized(
				"42", 10,
				Duration.ofSeconds(10), false,
				false,
				serviceSupplier
			);

			var run2 = exhaustive(serviceSupplier, generators);
			List<PropertyRunResult> results = propertyRun.run(List.of(run1, run2, run1));

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
				System.out.println(anInt);
				return anInt < 42;
			});

			PropertyRun propertyRun = new PropertyRun(generators, tryable);

			List<SampleRecording> samples = List.of(
				new SampleRecording(Recording.choice(10)),
				new SampleRecording(Recording.choice(13)),
				new SampleRecording(Recording.choice(27))
			);
			var run1 = samples(samples, serviceSupplier);

			var run2 = exhaustive(serviceSupplier, generators);

			List<PropertyRunResult> results = propertyRun.run(List.of(run1, run2, run1));

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

			PropertyRun propertyRun = new PropertyRun(generators, tryable);

			PropertyRunResult result = propertyRun.run(
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

			PropertyRun propertyRun = new PropertyRun(generators, tryable);

			PropertyRunResult result = propertyRun.run(
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

			PropertyRun propertyRun = new PropertyRun(generators, tryable);

			PropertyRunResult result = propertyRun.run(
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

			PropertyRun propertyRun = new PropertyRun(generators, tryable);

			PropertyRunResult result = propertyRun.run(
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

			PropertyRun propertyRun = new PropertyRun(generators, tryable);

			PropertyRunResult result = propertyRun.run(
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

			PropertyRun propertyRun = new PropertyRun(generators, tryable);

			PropertyRunResult result = propertyRun.run(
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

			PropertyRun propertyRun = new PropertyRun(generators, tryable);

			PropertyRunResult result = propertyRun.run(
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
