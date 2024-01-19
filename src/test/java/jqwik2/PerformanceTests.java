package jqwik2;

import java.util.*;

import jqwik2.api.*;
import jqwik2.internal.*;
import jqwik2.internal.generators.*;
import jqwik2.internal.recording.*;

import net.jqwik.api.*;

class PerformanceTests {

	// @Example
	void testXorShiftGenerator() throws Exception {
		java.util.random.RandomGenerator generator = new XORShiftRandom();
		int count = 100_000_000;
		PerformanceTesting.time("XorShift", count, () -> {
			generator.nextDouble();
			// System.out.println(generator.nextInt(5));
		});
	}

	@Example
	void compare_jqwik2poc_with_jqwik() throws Exception {
		IntegerGenerator randomInteger = new IntegerGenerator(-10, 100);
		ListGenerator<Integer> randomList = new ListGenerator<>(randomInteger, 0, 100);
		Generator<List<Integer>> randomListWithEdgeCases = WithEdgeCasesDecorator.decorate(randomList, 0.05, 10);

		int count = 100_000;

		PerformanceTesting.time("jqwik2 generation", count, () -> {
			GenRecorder source = new GenRecorder(new RandomGenSource());
			randomListWithEdgeCases.generate(source);
		});
		// System.out.println(source.recording());

		RandomGenerator<List<Integer>> generator = Arbitraries.integers().between(-10, 100)
															  .list().ofMaxSize(100)
															  .generator(1000);

		Random random = new Random();
		PerformanceTesting.time("jqwik1 generation", count, () -> generator.next(random));
	}

}
