package jqwik2;

import java.time.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.function.*;

import jqwik2.api.Assume;
import jqwik2.api.*;
import jqwik2.api.PropertyRunResult.*;
import jqwik2.internal.*;
import jqwik2.internal.generators.*;

import net.jqwik.api.*;

import static jqwik2.api.PropertyRunConfiguration.*;
import static org.assertj.core.api.Assertions.*;

class RunningPropertyTests {

	@Example
	void runSuccessfulProperty() {
		List<Generator<?>> generators = List.of(
			new IntegerGenerator(0, 100)
		);
		Tryable tryable = Tryable.from(args -> true);

		PropertyCase propertyCase = new PropertyCase(generators, tryable);

		PropertyRunResult result = propertyCase.run(
			randomized("42", 10, false, 0.0)
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
			randomized("42", 10, false, 0.0)
		);
		assertThat(result.status()).isEqualTo(Status.SUCCESSFUL);
		assertThat(result.countTries()).isEqualTo(10);
		assertThat(result.countChecks()).isEqualTo(10);
	}

	@Example
	void runSuccessfulWithInvalids() {
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
			randomized("42", 10, false, 0.0)
		);
		assertThat(result.status()).isEqualTo(Status.SUCCESSFUL);
		assertThat(result.countTries()).isEqualTo(10);
		// 5 invalids - depends on random seed
		assertThat(result.countChecks()).isEqualTo(5);
	}

	@Example
	void failProperty() {
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
			randomized("42", 10, false, 0.0)
		);
		assertThat(result.status()).isEqualTo(Status.FAILED);
		assertThat(result.countTries()).isEqualTo(1);
		assertThat(result.countChecks()).isEqualTo(1);
		assertThat(result.falsifiedSamples()).hasSizeGreaterThanOrEqualTo(1);
		FalsifiedSample smallest = result.falsifiedSamples().getFirst();
		assertThat(smallest.values())
			.isEqualTo(List.of(lastArg[0]));
		assertThat(smallest.thrown()).isEmpty();
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
			randomized("42", 10, false, 0.0)
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

	@Example
	void failAndShrink() {
		List<Generator<?>> generators = List.of(
			new IntegerGenerator(0, 100)
		);
		Tryable tryable = Tryable.from(args -> {
			int anInt = (int) args.get(0);
			return anInt < 45;
		});

		PropertyCase propertyCase = new PropertyCase(generators, tryable);

		PropertyRunResult result = propertyCase.run(
			randomized("4242", 100, true, 0.0)
		);
		assertThat(result.status()).isEqualTo(Status.FAILED);
		assertThat(result.countTries()).isEqualTo(2); // depends on seed
		assertThat(result.falsifiedSamples()).hasSizeGreaterThan(1);
		FalsifiedSample smallest = result.falsifiedSamples().getFirst();
		assertThat(smallest.values()).isEqualTo(List.of(45));
		FalsifiedSample biggest = result.falsifiedSamples().getLast();
		assertThat(biggest.values()).isEqualTo(List.of(55)); // depends on seed
	}

	@Example
	void runSuccessfulPropertyInCachedThreadPool() {
		// Should run in not much longer than 100 ms

		List<Generator<?>> generators = List.of(
			new IntegerGenerator(0, 100)
		);
		Tryable tryable = Tryable.from(args -> {
			try {
				Thread.sleep(100);
			} catch (InterruptedException e) {
				throw new RuntimeException(e);
			}
			return true;
		});

		PropertyCase propertyCase = new PropertyCase(generators, tryable);

		PropertyRunResult result = propertyCase.run(
			randomized(
				"42", 1000, false, 0.0,
				Duration.ofSeconds(10),
				Executors::newCachedThreadPool
			)
		);
		assertThat(result.status()).isEqualTo(Status.SUCCESSFUL);
		assertThat(result.countTries()).isEqualTo(1000);
		assertThat(result.countChecks()).isEqualTo(1000);
	}

	@Example
	void runSuccessfulWithMaxDuration() {

		List<Generator<?>> generators = List.of(
			new IntegerGenerator(0, 100)
		);
		Tryable tryable = Tryable.from(args -> {
			try {
				Thread.sleep(100);
			} catch (InterruptedException e) {
				throw new RuntimeException(e);
			}
			return true;
		});

		PropertyCase propertyCase = new PropertyCase(generators, tryable);

		PropertyRunResult result = propertyCase.run(
			randomized(
				"42", 1000, false, 0.0,
				Duration.ofSeconds(1),
				Executors::newSingleThreadExecutor
			)
		);
		assertThat(result.status()).isEqualTo(Status.SUCCESSFUL);
		assertThat(result.timedOut()).isTrue();
		assertThat(result.countTries()).isGreaterThan(0);
		assertThat(result.countChecks()).isLessThanOrEqualTo(result.countTries());
	}

	@Example
	void abortWithTimeout() {

		Tryable tryable = Tryable.from(args -> {
			try {
				Thread.sleep(1000);
			} catch (InterruptedException e) {
				throw new RuntimeException(e);
			}
		});

		PropertyCase propertyCase = new PropertyCase(List.of(new IntegerGenerator(0, 100)), tryable);

		PropertyRunResult result = propertyCase.run(
			randomized(
				"42", 1000, false, 0.0,
				Duration.ofMillis(100),
				Executors::newSingleThreadExecutor
			)
		);
		assertThat(result.status()).isEqualTo(Status.ABORTED);
		assertThat(result.abortionReason()).isPresent();
		assertThat(result.timedOut()).isTrue();
	}

	@Example
	void failAndShrinkInCachedThreadPool() {
		List<Generator<?>> generators = List.of(
			new IntegerGenerator(0, 100)
		);
		Tryable tryable = Tryable.from(args -> {
			int anInt = (int) args.get(0);
			return anInt < 20;
		});

		PropertyCase propertyCase = new PropertyCase(generators, tryable);

		PropertyRunResult result = propertyCase.run(
			randomized(
				"42", 10, true, 0.0,
				Duration.ofSeconds(10),
				Executors::newCachedThreadPool
			)
		);
		assertThat(result.status()).isEqualTo(Status.FAILED);
		FalsifiedSample smallest = result.falsifiedSamples().getFirst();
		assertThat(smallest.values()).isEqualTo(List.of(20));
	}

	@Property(tries = 10)
	void reproduceSameSamplesWithSameSeed(@ForAll long seed) {
		reproduceSameSamplesTwice(
			seed,
			0.0,
			Executors::newCachedThreadPool
		);
	}

	@Property(tries = 10)
	void reproduceSameSamplesWithSingleThreadExecutor(@ForAll long seed) {
		reproduceSameSamplesTwice(
			seed,
			0.0,
			Executors::newSingleThreadExecutor
		);
	}

	@Property(tries = 10)
	void reproduceSameSamplesEvenWithEdgeCases(@ForAll long seed) {
		reproduceSameSamplesTwice(
			seed,
			0.5,
			Executors::newCachedThreadPool
		);
	}

	private static void reproduceSameSamplesTwice(long seed, double edgeCasesProbability, Supplier<ExecutorService> serviceSupplier) {
		List<Generator<?>> generators = List.of(
			new IntegerGenerator(0, 100)
		);
		Tryable tryable = Tryable.from(args -> true);

		PropertyCase propertyCase = new PropertyCase(generators, tryable);

		final Set<Sample> samples1 = Collections.synchronizedSet(new HashSet<>());
		propertyCase.onSuccessful(samples1::add);

		PropertyRunConfiguration runConfiguration = randomized(
			Long.toString(seed), 10, false, edgeCasesProbability,
			Duration.ofSeconds(10),
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
