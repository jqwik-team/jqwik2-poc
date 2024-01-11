package jqwik2;

import jqwik2.api.*;
import jqwik2.api.Arbitrary;
import org.assertj.core.api.*;

import net.jqwik.api.*;

import static org.assertj.core.api.Assertions.*;

class ArbitraryTests {

	@Property(tries = 10)
	void anIntegerArbitrary() {
		for (int i = 0; i < 10; i++) {
			Arbitrary<Integer> ints = net.jqwik.api.arbitraries.Numbers.integers();
			int sample = ints.sample();
			// System.out.println(sample);
			assertThat(sample).isBetween(Integer.MIN_VALUE, Integer.MAX_VALUE);
		}
	}
}
