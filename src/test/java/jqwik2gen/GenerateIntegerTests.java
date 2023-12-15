package jqwik2gen;

import org.assertj.core.api.*;

import net.jqwik.api.*;

class GenerateIntegerTests {

	@Example
	void smallPositive() {
		Generator<Integer> gen0to10 = new IntegerGenerator(-10, 100);

		RandomGenSource source = new RandomGenSource(42);

		for (int i = 0; i < 10; i++) {
			Shrinkable<Integer> shrinkable = gen0to10.generate(source);
			Assertions.assertThat(shrinkable).isNotNull();
			System.out.println("value=" + shrinkable.value());


		}
	}
}
