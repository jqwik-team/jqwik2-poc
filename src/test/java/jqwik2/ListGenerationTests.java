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
	void regenerateValue() {
		IntegerGenerator randomInteger = new IntegerGenerator(-10, 100);
		ListGenerator<Integer> randomList = new ListGenerator<>(randomInteger, 0, 5);

		GenerationSource randomSource = new RandomSource();

		for (int i = 0; i < 10; i++) {
			GeneratedValue<List<Integer>> generated = randomList.generate(randomSource);
			GeneratedValue<List<Integer>> regenerated = generated.regenerate();

			System.out.println("gen  : " + generated.value());
			System.out.println("regen: " + regenerated.value());
		}
	}

	@Example
	void generateRandomlyWithEdgeCases() {
		IntegerGenerator randomInteger = new IntegerGenerator(-10, 100);
		ListGenerator<Integer> randomList = new ListGenerator<>(randomInteger, 0, 5);

		List<List<Integer>> edgeCases = List.of(
			List.of(),
			List.of(0),
			List.of(-1),
			List.of(1)
		);

		EdgeCasesDecorator<List<Integer>> listGenerator = new EdgeCasesDecorator<>(
			randomList,
			edgeCases
		);

		for (int i = 0; i < 10; i++) {
			System.out.println(listGenerator.generate(new RandomSource()));
		}
	}

	@Example
	void performance() {
		IntegerGenerator randomInteger = new IntegerGenerator(-10, 100);
		ListGenerator<Integer> randomList = new ListGenerator<>(randomInteger, 0, 5);
		GenerationSource randomSource = new RandomSource();

		int count = 100000;

		time("new", count, () -> randomList.generate(randomSource));

		RandomGenerator<List<Integer>> generator = Arbitraries.integers().between(-10, 100)
															  .list().ofMinSize(0).ofMaxSize(5)
															  .generator(1000);

		Random random = new Random();
		time("old", count, () -> generator.next(random));
	}

	private void time(String label, int count, Runnable runnable) {
		long start = System.currentTimeMillis();
		for (int i = 0; i < count; i++) {
			runnable.run();
		}
		long end = System.currentTimeMillis();
		System.out.println(label + " Time: " + (end - start) + " ms");
	}

}
