package jqwik2;

import net.jqwik.api.*;

class IntegerGenerationTests {

	@Example
	void generateRandomly() {
		IntegerGenerator randomInteger = new IntegerGenerator();

		GenerationSource randomSource = new RandomSource();

		for (int i = 0; i < 10; i++) {
			System.out.println(randomInteger.value(randomSource));
		}
	}

	@Example
	void generateWithFixedSource() {
		IntegerGenerator randomInteger = new IntegerGenerator();

		GenerationSource randomSource = new FixedGenerationSource(0, 1, -1, Integer.MAX_VALUE, Integer.MIN_VALUE);

		for (int i = 0; i < 10; i++) {
			System.out.println(randomInteger.value(randomSource));
		}
	}

}
