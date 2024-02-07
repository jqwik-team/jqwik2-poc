package jqwik2;

import jqwik2.api.*;
import jqwik2.api.arbitraries.*;
import jqwik2.api.statistics.*;

import net.jqwik.api.*;

import static jqwik2.api.PropertyRunStrategy.GenerationMode.*;

class StatisticsTests {

	@Example
	void collectSingleValue() {
		var property = new JqwikProperty()
						   .withGeneration(RANDOMIZED)
						   .withEdgeCases(PropertyRunStrategy.EdgeCasesMode.OFF)
						   .withMaxTries(1000);

		property.onFailed((r, e) -> {
			throw new RuntimeException("Property failed: " + r, e);
		});

		Collector.C1<Integer> collector = Collector.create("numbers", Integer.class);

		IntegerArbitrary integers = Numbers.integers().between(0, 100);
		PropertyRunResult result = property.forAll(integers).check(i -> {
			collector.collect(i);
			return true;
		});

		for (Integer value : collector.values()) {
			System.out.println("Value " + value + " occurred " + collector.count(value) + " times");
		}
	}

}
