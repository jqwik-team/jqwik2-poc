package jqwik2;

import java.util.*;

import net.jqwik.api.*;

class PerformanceTests {


	@Example
	void performance() {
		IntegerGenerator randomInteger = new IntegerGenerator(-10, 100);
		ListGenerator<Integer> randomList = new ListGenerator<>(randomInteger, 10);

		int count = 1_000_000;

		GenRecorder source = new GenRecorder(new RandomGenSource());
		time("jqwik2", count, () -> {
			randomList.generate(source);
		});
		// System.out.println(source.recording());

		RandomGenerator<List<Integer>> generator = Arbitraries.integers().between(-10, 100)
															  .list().ofMaxSize(10)
															  .generator(1000);

		Random random = new RandomChoice.XORShiftRandom();
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
