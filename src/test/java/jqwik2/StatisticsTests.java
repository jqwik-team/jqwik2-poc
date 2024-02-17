package jqwik2;

import java.time.*;
import java.util.concurrent.atomic.*;

import jqwik2.api.*;
import jqwik2.api.arbitraries.*;
import jqwik2.api.statistics.*;

import net.jqwik.api.*;

import static jqwik2.api.PropertyRunStrategy.GenerationMode.*;
import static org.assertj.core.api.Assertions.*;

class StatisticsTests {

	@Example
	void collectSingleValue() {
		var property = new JqwikProperty()
						   .withGeneration(RANDOMIZED)
						   .withEdgeCases(PropertyRunStrategy.EdgeCasesMode.OFF)
						   .withMaxTries(1000)
						   .withMaxRuntime(Duration.ZERO);

		property.onFailed((r, e) -> {
			throw new RuntimeException("Property failed: " + r, e);
		});

		Statistics.Collector.C1<Integer> collector = Statistics.collector("numbers", Integer.class);

		IntegerArbitrary integers = Numbers.integers().between(0, 100);
		PropertyRunResult result = property.forAll(integers).check(i -> {
			collector.collect(i);
			return true;
		});

		// TODO: Check for reasonable values in collector
		// for (Integer value : collector.values()) {
		// 	System.out.println("Value " + value + " occurred " + collector.count(value) + " times");
		// }
	}

	@Example
	void statisticalCheckSuccessful() {
		var property = new JqwikProperty()
						   .withGeneration(RANDOMIZED)
						   .withEdgeCases(PropertyRunStrategy.EdgeCasesMode.OFF)
						   .withMaxTries(0)
						   .withMaxRuntime(Duration.ZERO);

		IntegerArbitrary integers = Numbers.integers().between(0, 100);

		AtomicInteger countEven = new AtomicInteger(0);
		PropertyRunResult result = property.forAll(integers).verify(
			i -> {
				if (i % 2 == 0) {
					countEven.incrementAndGet();
				}
			},
			// TODO: API is wrong. The predicate should be i % 2.
			// check("blabla", i % 2 == 0, p -> p > 0.48)
			Statistics.check("Even number occurs at least 48%", n -> countEven.get() / n > 0.48)
		);
		System.out.println(countEven.get() + " of " + result.countChecks());
		assertThat(result.isSuccessful()).isTrue();
	}

	@Example
	void statisticalCheckFailed() {
		var property = new JqwikProperty()
						   .withGeneration(RANDOMIZED)
						   .withEdgeCases(PropertyRunStrategy.EdgeCasesMode.OFF)
						   .withMaxTries(0)
						   .withMaxRuntime(Duration.ZERO);

		IntegerArbitrary integers = Numbers.integers().between(0, 100);

		AtomicInteger countEven = new AtomicInteger(0);
		PropertyRunResult result = property.forAll(integers).verify(
			i -> {
				if (i % 2 == 0) {
					countEven.incrementAndGet();
				}
			},
			Statistics.check("Even number occurs at least 52%", n -> countEven.get() / n > 0.52)
		);

		System.out.println(countEven.get() + " of " + result.countChecks());
		System.out.println(result.failureReason().get().getMessage());
		assertThat(result.isFailed()).isTrue();
	}

}
