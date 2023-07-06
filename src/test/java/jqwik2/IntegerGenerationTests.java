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

	@Example
	void generateRandomlyWithBounds() {
		IntegerGenerator randomInteger = new IntegerGenerator(-10, 100);

		GenerationSource randomSource = new RandomSource();

		for (int i = 0; i < 10; i++) {
			System.out.println(randomInteger.value(randomSource));
		}
	}


	@Example
	@Disabled("exhaustive generation is not implemented yet")
	void exhaustiveGeneration() {
		IntegerGenerator integerGenerator = new IntegerGenerator(-3, 5);
		ExhaustiveGenerationSource exhaustiveSource = new ExhaustiveGenerationSource();

		for (int i = 0; i < 5; i++) {
			System.out.println(integerGenerator.value(exhaustiveSource));
		}

	}


	@Group
	class WithEdgeCases {

		@Example
		void generateRandomlyWithEdgeCases() {
			IntegerGenerator baseGenerator = new IntegerGenerator(-10, 100);

			EdgeCasesDecorator<Integer> integerGenerator = new EdgeCasesDecorator<>(baseGenerator, 0, 1, -1, 100, -10);

			for (int i = 0; i < 10; i++) {
				System.out.println(integerGenerator.value(new RandomSource()));
			}
		}

		@Example
		void generateOnlyEdgeCasesWithFixedSource() {
			IntegerGenerator baseGenerator = new IntegerGenerator(-10, 100);

			EdgeCasesDecorator<Integer> integerGenerator = new EdgeCasesDecorator<>(baseGenerator, 0, 1, -1, 100, -10);

			GenerationSource randomSource = new FixedGenerationSource(
				1, 0,
				1, 1,
				1, 2,
				1, 3,
				1, 4
			);

			for (int i = 0; i < 5; i++) {
				System.out.println(integerGenerator.value(randomSource));
			}
		}

	}

}
