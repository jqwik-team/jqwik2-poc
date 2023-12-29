package jqwik2;

import java.util.*;

import jqwik2.api.*;

import net.jqwik.api.*;

class PerformanceTests {

	// @Example
	void testXorShiftGenerator() {
		java.util.random.RandomGenerator generator = new XORShiftRandom();
		int count = 100_000_000;
		time("XorShift", count, () -> {
			generator.nextDouble();
			// System.out.println(generator.nextInt(5));
		});
	}

	@Example
	void compare_jqwik2poc_with_jqwik() {
		IntegerGenerator randomInteger = new IntegerGenerator(-10, 100);
		ListGenerator<Integer> randomList = new ListGenerator<>(randomInteger, 100);
		Generator<List<Integer>> randomListWithEdgeCases = WithEdgeCasesDecorator.decorate(randomList, 0.05, 10);

		int count = 100_000;

		time("jqwik2", count, () -> {
			GenRecorder source = new GenRecorder(new RandomGenSource());
			randomListWithEdgeCases.generate(source);
		});
		// System.out.println(source.recording());

		RandomGenerator<List<Integer>> generator = Arbitraries.integers().between(-10, 100)
															  .list().ofMaxSize(100)
															  .generator(1000);

		Random random = new Random();
		time("jqwik1", count, () -> generator.next(random));
	}

	private void time(String label, int count, Runnable runnable) {
		long start = System.currentTimeMillis();
		for (int i = 0; i < count; i++) {
			runnable.run();
		}
		long end = System.currentTimeMillis();
		System.out.printf("[%s] Time: %d ms%n", label, end - start);
	}

}
