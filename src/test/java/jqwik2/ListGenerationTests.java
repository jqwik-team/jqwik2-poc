package jqwik2;

import net.jqwik.api.*;

public class ListGenerationTests {

	@Example
	void generateRandomly() {
		IntegerGenerator randomInteger = new IntegerGenerator(-10, 100);
		ListGenerator<Integer> randomList = new ListGenerator<>(randomInteger, 0, 5);

		GenerationSource randomSource = new RandomSource();

		for (int i = 0; i < 10; i++) {
			System.out.println(randomList.value(randomSource));
		}
	}

}
