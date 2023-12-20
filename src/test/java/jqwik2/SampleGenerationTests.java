package jqwik2;

import java.util.*;

import net.jqwik.api.*;

import static org.assertj.core.api.Assertions.*;

class SampleGenerationTests {

	@Example
	void generateRandomSample() {
		Generator<Integer> ints = new IntegerGenerator(-100, 100);
		Generator<List<Integer>> lists = new ListGenerator<>(ints, 5);

		GenRecorder recorder1 = new GenRecorder(new RandomGenSource("41"));
		GenRecorder recorder2 = new GenRecorder(new RandomGenSource("42"));

		List<Shrinkable<Object>> shrinkables = List.of(
			ints.generate_OLD(recorder1).asGeneric(),
			lists.generate_OLD(recorder2).asGeneric()
		);

		Sample sample = new Sample(shrinkables);

		System.out.println("sample = " + sample);

		List<Object> regeneratedValues = sample.regenerateValues();
		assertThat(regeneratedValues).isEqualTo(sample.values());
	}

	@Example
	void useRandomSampleGenerator() {
		Generator<Integer> ints = new IntegerGenerator(-100, 100);
		Generator<List<Integer>> lists = new ListGenerator<>(ints, 5);

		for (int i = 0; i < 10; i++) {

			Sample sample = new SampleGenerator(List.of(ints, lists))
								.generateRandomly(new RandomGenSource());

			System.out.println("sample = " + sample.values());

			List<Object> regeneratedValues = sample.regenerateValues();
			assertThat(regeneratedValues).isEqualTo(sample.values());
		}
	}
}
