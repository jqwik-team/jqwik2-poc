package jqwik2;

import java.time.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;
import java.util.function.*;

import jqwik2.api.Assume;
import jqwik2.api.*;
import jqwik2.api.PropertyRunResult.*;
import jqwik2.internal.*;
import jqwik2.internal.generators.*;

import net.jqwik.api.Arbitrary;
import net.jqwik.api.*;

import static jqwik2.internal.PropertyRunConfiguration.*;
import static org.assertj.core.api.Assertions.*;

class PropertyCaseTests {

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

		PropertyCase propertyCase = new PropertyCase(generators, tryable);

		PropertyRunResult result = propertyCase.run(
			randomized(
				"42", 10,
				false,
				Duration.ofSeconds(10),
				false,
				serviceSupplier
			)
		);
		assertThat(result.status()).isEqualTo(Status.SUCCESSFUL);
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

		PropertyCase propertyCase = new PropertyCase(generators, tryable);

		PropertyRunResult result = propertyCase.run(
			randomized("42", 10, false, false)
		);
		assertThat(result.status()).isEqualTo(Status.SUCCESSFUL);
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

		PropertyCase propertyCase = new PropertyCase(generators, tryable);

		PropertyRunResult result = propertyCase.run(
			randomized(
				"42", 10,
				false,
				Duration.ofSeconds(10),
				false,
				serviceSupplier
			)
		);
		assertThat(result.status()).isEqualTo(Status.SUCCESSFUL);
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

		PropertyCase propertyCase = new PropertyCase(generators, tryable);

		PropertyRunResult result = propertyCase.run(
			randomized(
				"42", 10,
				false,
				Duration.ofSeconds(10),
				false,
				serviceSupplier
			)
		);
		assertThat(result.status()).isEqualTo(Status.FAILED);
		assertThat(result.countTries()).isGreaterThanOrEqualTo(1);
		assertThat(result.countChecks()).isEqualTo(result.countTries());
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

		PropertyCase propertyCase = new PropertyCase(generators, tryable);

		PropertyRunResult result = propertyCase.run(
			randomized(
				"42", 10,
				true,
				Duration.ofSeconds(10),
				false,
				serviceSupplier
			)
		);
		assertThat(result.status()).isEqualTo(Status.FAILED);
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

		PropertyCase propertyCase = new PropertyCase(generators, tryable);

		PropertyRunResult result = propertyCase.run(
			randomized("42", 10, false, false)
		);
		assertThat(result.status()).isEqualTo(Status.FAILED);
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

		PropertyCase propertyCase = new PropertyCase(generators, tryable);

		PropertyRunResult result = propertyCase.run(
			randomized(
				"4242", 100,
				true,
				Duration.ofSeconds(10),
				false,
				serviceSupplier
			)
		);
		assertThat(result.status()).isEqualTo(Status.FAILED);
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

		PropertyCase propertyCase = new PropertyCase(generators, tryable);

		PropertyRunResult result = propertyCase.run(
			randomized(
				"42", 1000, false,
				Duration.ofSeconds(1), false,
				serviceSupplier
			)
		);
		assertThat(result.status()).isEqualTo(Status.SUCCESSFUL);
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
		PropertyCase propertyCase = new PropertyCase(List.of(anyGenerator), tryable);

		PropertyRunResult result = propertyCase.run(
			randomized(
				"42", 100, false,
				Duration.ofMillis(200), false,
				serviceSupplier
			)
		);
		assertThat(result.status()).isIn(Status.FAILED, Status.ABORTED);
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

		PropertyCase propertyCase = new PropertyCase(List.of(aFailingGenerator), tryable);

		String seed = RandomChoice.generateRandomSeed();
		PropertyRunResult result = propertyCase.run(
			randomized(
				seed, 100, true,
				Duration.ofMinutes(10),
				false,
				serviceSupplier
			)
		);
		assertThat(result.status()).isEqualTo(Status.ABORTED);
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

		PropertyCase propertyCase = new PropertyCase(generators, tryable);

		final Set<Sample> samples1 = Collections.synchronizedSet(new HashSet<>());
		propertyCase.onSuccessful(samples1::add);

		PropertyRunConfiguration runConfiguration = randomized(
			Long.toString(seed), 10, false,
			Duration.ofSeconds(10), false,
			serviceSupplier
		);

		assertThat(propertyCase.run(runConfiguration).status())
			.isEqualTo(Status.SUCCESSFUL);

		final Set<Sample> samples2 = Collections.synchronizedSet(new HashSet<>());
		propertyCase.onSuccessful(samples2::add);
		assertThat(propertyCase.run(runConfiguration).status())
			.isEqualTo(Status.SUCCESSFUL);

		assertThat(samples1).hasSameElementsAs(samples2);
	}

}
