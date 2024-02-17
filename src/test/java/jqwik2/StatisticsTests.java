package jqwik2;

import java.time.*;
import java.util.concurrent.atomic.*;

import jqwik2.api.*;
import jqwik2.api.arbitraries.*;
import jqwik2.api.statistics.*;

import net.jqwik.api.*;
import net.jqwik.api.lifecycle.*;

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

		Collector.C1<Integer> collector = Collector.create("numbers", Integer.class);

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
			Checker.check("Even number occurs at least 40%", n -> countEven.get() / n > 0.4)
		);
		// System.out.println(countEven.get());
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
			Checker.check("Even number occurs at least 80%", n -> countEven.get() / n > 0.8)
		);

		// System.out.println(result.failureReason().get().getMessage());
		assertThat(result.isFailed()).isTrue();
	}

}
