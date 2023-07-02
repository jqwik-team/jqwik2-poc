package jqwik2;

import net.jqwik.api.*;

class IntegerGenerationTests {

	@Example
	void generateRandomly() {
		RandomInteger randomInteger = new RandomInteger();

		RandomSource randomSource = new DefaultRandomSource();

		for (int i = 0; i < 10; i++) {
			System.out.println(randomInteger.value(randomSource));
		}
	}

	@Example
	void generateWithFixedSource() {
		RandomInteger randomInteger = new RandomInteger();

		RandomSource randomSource = new FixedRandomSource(0, 1, -1, Integer.MAX_VALUE, Integer.MIN_VALUE);

		for (int i = 0; i < 10; i++) {
			System.out.println(randomInteger.value(randomSource));
		}
	}

}
