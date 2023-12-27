package jqwik2;

import java.util.*;

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
			"42",
			10,
			0.0,
			false
		);

		PropertyExecutionResult result = propertyCase.execute();
		assertThat(result.status()).isEqualTo(Status.SUCCESSFUL);
		assertThat(result.countTries()).isEqualTo(10);
		// 2 invalids - depends on random seed
		assertThat(result.countChecks()).isEqualTo(8);
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
		assertThat(result.falsifiedSamples()).hasSize(1);
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
			return anInt < 20;
		});

		PropertyCase propertyCase = new PropertyCase(
			generators,
			tryable,
			"42",
			100,
			0.0,
			true
		);

		PropertyExecutionResult result = propertyCase.execute();
		assertThat(result.status()).isEqualTo(Status.FAILED);
		assertThat(result.countTries()).isEqualTo(2); // depends on seed
		assertThat(result.falsifiedSamples()).hasSizeGreaterThanOrEqualTo(2);
		FalsifiedSample smallest = result.falsifiedSamples().getFirst();
		assertThat(smallest.values()).isEqualTo(List.of(20));
		FalsifiedSample biggest = result.falsifiedSamples().getLast();
		assertThat(biggest.values()).isEqualTo(List.of(68)); // depends on seed
	}

}
