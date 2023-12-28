package jqwik2;

import java.util.*;

import jqwik2.api.Shrinkable;
import jqwik2.api.*;

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
			new GeneratedShrinkable<>(ints.generate(recorder1), ints, recorder1.recording()).asGeneric(),
			new GeneratedShrinkable<>(lists.generate(recorder2), lists, recorder2.recording()).asGeneric()
		);

		Sample sample = new Sample(shrinkables);
		// System.out.println("sample = " + sample);

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
			// System.out.println("sample = " + sample.values());

			List<Object> regeneratedValues = sample.regenerateValues();
			assertThat(regeneratedValues).isEqualTo(sample.values());
		}
	}

	@Example
	void useRandomSampleGeneratorWithEdgeCases() {
		Generator<Integer> ints = new IntegerGenerator(-100, 100);

		Set<Integer> values = new HashSet<>();
		for (int i = 0; i < 100; i++) {
			Sample sample = new SampleGenerator(List.of(ints), 0.9, 10)
								.generateRandomly(new RandomGenSource());
			values.add((Integer) sample.values().getFirst());
		}
		assertThat(values).contains(
			0, 1, -1, 100, -100
		);
	}

	@Example
	void generateWithEdgeCases() {
		Generator<Integer> ints = new IntegerGenerator(-100, 100);
		Generator<List<Integer>> lists = new ListGenerator<>(ints, 5);

		for (int i = 0; i < 100; i++) {
			Sample sample = new SampleGenerator(
				List.of(ints, lists),
				0.5, 100
			).generateRandomly(new RandomGenSource());

			System.out.println("sample = " + sample.values());

			List<Object> regeneratedValues = sample.regenerateValues();
			assertThat(regeneratedValues).isEqualTo(sample.values());
		}
	}

}
