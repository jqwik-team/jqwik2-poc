package jqwik2;

import java.time.*;

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
		assertThat(strategy.shrinking()).isEqualTo(PropertyRunStrategy.Shrinking.FULL);
		assertThat(strategy.generation()).isEqualTo(PropertyRunStrategy.Generation.RANDOMIZED);
	}

	@Example
	void exhaustiveProperty() {
		PropertyRunStrategy strategy = PropertyRunStrategy.create(
			100, Duration.ofMinutes(10), null,
			PropertyRunStrategy.Shrinking.OFF,
			PropertyRunStrategy.Generation.EXHAUSTIVE
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


}
