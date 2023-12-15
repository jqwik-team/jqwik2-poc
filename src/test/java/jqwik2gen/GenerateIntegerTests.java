package jqwik2gen;

import java.util.*;

import net.jqwik.api.*;

import static org.assertj.core.api.Assertions.*;

class GenerateIntegerTests {

	@Example
	void smallInts() {
		Generator<Integer> gen0to10 = new IntegerGenerator(-10, 100);

		RandomGenSource source = new RandomGenSource(42);

		for (int i = 0; i < 10; i++) {
			Shrinkable<Integer> shrinkable = gen0to10.generate(source);
			assertThat(shrinkable).isNotNull();
			System.out.println("value=" + shrinkable.value());

			Integer regenerated = shrinkable.regenerate();
			System.out.println("regenerated=" + regenerated);

			assertThat(regenerated).isEqualTo(shrinkable.value());
		}
	}

	@Example
	void listOfInts() {
		IntegerGenerator ints = new IntegerGenerator(-10, 100);
		Generator<List<Integer>> listOfInts = new ListGenerator(ints, 5);

		RandomGenSource source = new RandomGenSource(42);

		for (int i = 0; i < 10; i++) {
			Shrinkable<List<Integer>> shrinkable = listOfInts.generate(source);
			System.out.println("value=" + shrinkable.value());

			List<Integer> regenerated = shrinkable.regenerate();
			System.out.println("regenerated=" + regenerated);

			assertThat(regenerated).isEqualTo(shrinkable.value());
		}
	}
}
