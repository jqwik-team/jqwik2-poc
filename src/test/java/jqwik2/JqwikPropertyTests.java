package jqwik2;

import java.time.*;
import java.util.*;

import jqwik2.api.*;
import jqwik2.api.arbitraries.*;

import net.jqwik.api.*;

import static org.assertj.core.api.Assertions.*;

class JqwikPropertyTests {

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
	void propertyWith1ParameterFails() {
		var property = new JqwikProperty();

		PropertyRunResult result = property.forAll(Numbers.integers()).check(i -> false);
		assertThat(result.isFailed()).isTrue();
		assertThat(result.countTries()).isEqualTo(1);
		assertThat(result.countChecks()).isEqualTo(1);

		result = property.forAll(Numbers.integers()).verify(i -> {
			// Thread.sleep(10);
			// System.out.println(i);
			throw new AssertionError("failed");
		});
		assertThat(result.isFailed()).isTrue();
		assertThat(result.countTries()).isEqualTo(1);
		assertThat(result.countChecks()).isEqualTo(1);
	}

	@Example
	void failingPropertyThrowIfNotSuccessful() {
		var property = new JqwikProperty();
		property.failIfNotSuccessful(true);

		assertThatThrownBy(
			() -> property.forAll(Numbers.integers()).check(i -> false)
		).isInstanceOf(AssertionError.class)
		 .hasMessageContaining("failed");
	}

	@Example
	void propertyWith2ParametersSucceeds() {
		var property = new JqwikProperty();

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
	void propertyDefaultStrategyReporting() {
		var property = new JqwikProperty();
		PropertyRunStrategy strategy = property.strategy();

		assertThat(strategy.maxTries()).isEqualTo(100);
		assertThat(strategy.maxRuntime()).isEqualTo(Duration.ofMinutes(10));
		assertThat(strategy.shrinking()).isEqualTo(PropertyRunStrategy.ShrinkingMode.FULL);
		assertThat(strategy.generation()).isEqualTo(PropertyRunStrategy.GenerationMode.RANDOMIZED);
	}

	@Example
	void exhaustiveGenerationStrategy() {
		PropertyRunStrategy strategy = PropertyRunStrategy.create(
			100, Duration.ofMinutes(10), null,
			PropertyRunStrategy.ShrinkingMode.OFF,
			PropertyRunStrategy.GenerationMode.EXHAUSTIVE,
			PropertyRunStrategy.EdgeCasesMode.OFF
		);
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
	void smartGenerationProperty() {
		PropertyRunStrategy strategy = PropertyRunStrategy.create(
			100, Duration.ofMinutes(10), RandomChoice.generateRandomSeed(),
			PropertyRunStrategy.ShrinkingMode.OFF,
			PropertyRunStrategy.GenerationMode.SMART,
			PropertyRunStrategy.EdgeCasesMode.MIXIN
		);
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
	void edgeCasesGenerationProperty() {
		PropertyRunStrategy strategy = PropertyRunStrategy.create(
			1000, Duration.ofMinutes(10), RandomChoice.generateRandomSeed(),
			PropertyRunStrategy.ShrinkingMode.OFF,
			PropertyRunStrategy.GenerationMode.SMART,
			PropertyRunStrategy.EdgeCasesMode.MIXIN
		);
		var property = new JqwikProperty(strategy);

		List<Integer> values = Collections.synchronizedList(new ArrayList<>());
		PropertyRunResult resultExhaustive = property.forAll(Numbers.integers()).verify(values::add);
		assertThat(resultExhaustive.isSuccessful()).isTrue();
		assertThat(resultExhaustive.countTries()).isEqualTo(1000);

		assertThat(values).contains(0, 1, -1, Integer.MAX_VALUE, Integer.MIN_VALUE);
	}

}
