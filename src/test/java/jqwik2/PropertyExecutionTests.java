package jqwik2;

import java.time.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.function.*;

import jqwik2.api.Assume;
import jqwik2.api.*;
import jqwik2.api.PropertyExecutionResult.*;

import net.jqwik.api.*;

import static org.assertj.core.api.Assertions.*;

class PropertyExecutionTests {

	@Example
	void runSuccessfulProperty() {
		List<Generator<?>> generators = List.of(
			new IntegerGenerator(0, 100)
		);
		Tryable tryable = Tryable.from(args -> true);

		PropertyCase propertyCase = new PropertyCase(
			generators,
			tryable,
			"42",
			10,
			0.0,
			false
		);

		PropertyExecutionResult result = propertyCase.execute();
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

		PropertyCase propertyCase = new PropertyCase(
			generators,
			tryable,
			"47",
			10,
			0.0,
			false
		);

		PropertyExecutionResult result = propertyCase.execute();
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

		PropertyCase propertyCase = new PropertyCase(
			generators,
			tryable,
			"42",
			10,
			0.0,
			false
		);

		PropertyExecutionResult result = propertyCase.execute();
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

		PropertyCase propertyCase = new PropertyCase(
			generators,
			tryable,
			"42",
			10,
			0.0,
			false
		);

		PropertyExecutionResult result = propertyCase.execute();
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

		PropertyCase propertyCase = new PropertyCase(
			generators,
			tryable,
			"424242",
			100,
			0.0,
			true
		);

		PropertyExecutionResult result = propertyCase.execute();
		assertThat(result.status()).isEqualTo(Status.FAILED);
		assertThat(result.countTries()).isEqualTo(2); // depends on seed
		assertThat(result.falsifiedSamples()).hasSizeGreaterThanOrEqualTo(2);
		FalsifiedSample smallest = result.falsifiedSamples().getFirst();
		assertThat(smallest.values()).isEqualTo(List.of(45));
		FalsifiedSample biggest = result.falsifiedSamples().getLast();
		assertThat(biggest.values()).isEqualTo(List.of(51)); // depends on seed
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

		PropertyCase propertyCase = new PropertyCase(
			generators,
			tryable,
			"42",
			1000,
			0.0,
			false,
			Duration.ofSeconds(10),
			Executors::newCachedThreadPool
		);

		PropertyExecutionResult result = propertyCase.execute();
		assertThat(result.status()).isEqualTo(Status.SUCCESSFUL);
		assertThat(result.countTries()).isEqualTo(1000);
		assertThat(result.countChecks()).isEqualTo(1000);
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

		PropertyCase propertyCase = new PropertyCase(
			generators,
			tryable,
			"42",
			1000,
			0.0,
			true,
			Duration.ofSeconds(10),
			Executors::newCachedThreadPool		);

		PropertyExecutionResult result = propertyCase.execute();
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

		PropertyCase propertyCase = new PropertyCase(
			generators,
			tryable,
			Long.toString(seed),
			10,
			edgeCasesProbability,
			false,
			Duration.ofSeconds(10),
			serviceSupplier
		);

		final Set<Sample> samples1 = Collections.synchronizedSet(new HashSet<>());
		propertyCase.onSuccessful(samples1::add);
		assertThat(propertyCase.execute().status()).isEqualTo(Status.SUCCESSFUL);

		final Set<Sample> samples2 = Collections.synchronizedSet(new HashSet<>());
		propertyCase.onSuccessful(samples2::add);
		assertThat(propertyCase.execute().status()).isEqualTo(Status.SUCCESSFUL);

		Sample first1 = samples1.iterator().next();
		Sample first2 = samples2.iterator().next();
		assertThat(samples1).hasSameElementsAs(samples2);
	}

}
