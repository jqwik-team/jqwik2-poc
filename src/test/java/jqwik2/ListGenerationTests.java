package jqwik2;

import net.jqwik.api.*;

import java.util.*;

public class ListGenerationTests {

	@Example
	void generateRandomly() {
		IntegerGenerator randomInteger = new IntegerGenerator(-10, 100);
		ListGenerator<Integer> randomList = new ListGenerator<>(randomInteger, 0, 5);

		GenerationSource randomSource = new RandomSource();

		for (int i = 0; i < 10; i++) {
			System.out.println(randomList.generate(randomSource));
		}
	}

	@Example
	void performance() {
		IntegerGenerator randomInteger = new IntegerGenerator(-10, 100);
		ListGenerator<Integer> randomList = new ListGenerator<>(randomInteger, 0, 5);
		GenerationSource randomSource = new RandomSource();

		int count = 100000;

		time(count, () -> randomList.generate(randomSource));

		RandomGenerator<List<Integer>> generator = Arbitraries.integers().between(-10, 100)
															  .list().ofMinSize(0).ofMaxSize(5)
															  .generator(1000);

		Random random = new Random();
		time(count, () -> generator.next(random));
	}

	private void time(int count, Runnable runnable) {
		long start = System.currentTimeMillis();
		for (int i = 0; i < count; i++) {
			runnable.run();
		}
		long end = System.currentTimeMillis();
		System.out.println("Time: " + (end - start) + " ms");
	}

}
