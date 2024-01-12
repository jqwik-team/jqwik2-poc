package jqwik2;

import jqwik2.api.Arbitrary;
import jqwik2.api.arbitraries.*;

import net.jqwik.api.*;

import static org.assertj.core.api.Assertions.*;

class ArbitraryTests {

	@Property(tries = 10)
	void anIntegerArbitrary() {
		for (int i = 0; i < 10; i++) {
			Arbitrary<Integer> ints = Numbers.integers();
			int sample = ints.sample();
			// System.out.println(sample);
			assertThat(sample).isBetween(Integer.MIN_VALUE, Integer.MAX_VALUE);
		}
	}
}
