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
		java.util.random.RandomGenerator xorShiftRandom = new XORShiftRandom();
		int count = 100_000_000;
		PerformanceTesting.time("XorShift", count, () -> {
			xorShiftRandom.nextDouble();
		});
		java.util.random.RandomGenerator javaUtilRandom = new Random();
		PerformanceTesting.time("java.util.Random", count, () -> {
			javaUtilRandom.nextDouble();
		});
	}

	@Example
	void compare_jqwik2poc_with_jqwik() throws Exception {
		IntegerGenerator randomInteger = new IntegerGenerator(-10, 100);
		ListGenerator<Integer> randomList = new ListGenerator<>(randomInteger, 0, 100);
		Generator<List<Integer>> randomListWithEdgeCases = WithEdgeCasesDecorator.decorate(randomList, 0.05, 10);

		int count = 100_000;

		PerformanceTesting.time("jqwik2 generation", count, () -> {
			// Recording slows generation down by factor of 2, but it's necessary for shrinking and reproducing failures
			GenRecorder source = new GenRecorder(new RandomGenSource());
			// GenSource source = new RandomGenSource();
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
