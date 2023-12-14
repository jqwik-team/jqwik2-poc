package jqwik2Gen;

import jqwik2gen.*;
import jqwik2gen.Shrinkable;
import org.assertj.core.api.*;

import net.jqwik.api.*;

class GenerateIntegerTests {

	@Example
	void smallPositive() {
		Generator<Integer> gen0to10 = new IntegerGen(0, 10);

		RandomSource source = new RandomSource(42);

		Shrinkable<Integer> shrinkable = gen0to10.generate(source);

		Assertions.assertThat(shrinkable).isNotNull();
	}
}
