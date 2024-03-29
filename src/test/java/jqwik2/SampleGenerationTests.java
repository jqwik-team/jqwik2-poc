package jqwik2;

import java.util.*;

import jqwik2.api.Shrinkable;
import jqwik2.api.*;
import jqwik2.internal.*;
import jqwik2.internal.generators.*;
import jqwik2.internal.recording.*;

import net.jqwik.api.*;

import static org.assertj.core.api.Assertions.*;

class SampleGenerationTests {

	@Example
	void generateRandomSample() {
		Generator<Integer> ints = new IntegerGenerator(-100, 100);
		Generator<List<Integer>> lists = new ListGenerator<>(ints, 0, 5);

		GenRecorder recorder1 = new GenRecorder(new RandomGenSource("41"));
		GenRecorder recorder2 = new GenRecorder(new RandomGenSource("42"));

		List<Shrinkable<Object>> shrinkables = List.of(
			new GeneratedShrinkable<>(ints.generate(recorder1), ints, recorder1).asGeneric(),
			new GeneratedShrinkable<>(lists.generate(recorder2), lists, recorder2).asGeneric()
		);

		Sample sample = new Sample(shrinkables);
		// System.out.println("sample = " + sample);

		List<Object> regeneratedValues = sample.regenerateValues();
		assertThat(regeneratedValues).isEqualTo(sample.values());
	}

	@Example
	void useRandomSampleGenerator() {
		Generator<Integer> ints = new IntegerGenerator(-100, 100);
		Generator<List<Integer>> lists = new ListGenerator<>(ints, 0, 5);

		RandomGenSource randomGenSource = new RandomGenSource();
		for (int i = 0; i < 10; i++) {
			SampleGenerator sampleGenerator = SampleGenerator.from(ints, lists);
			List<GenSource> sources = List.of(randomGenSource.split(), randomGenSource.split());
			Sample sample = sampleGenerator.generate(sources).orElseThrow();
			// System.out.println("sample = " + sample.values());

			List<Object> regeneratedValues = sample.regenerateValues();
			assertThat(regeneratedValues).isEqualTo(sample.values());
		}
	}

	@Example
	void sampleGeneratorCanFilterOutDuplicates() {
		Generator<Integer> ints = new IntegerGenerator(-10, 10);
		Generator<List<Integer>> lists = new ListGenerator<>(ints, 0, 3);

		List<Sample> allSamples = new ArrayList<>();

		RandomGenSource randomGenSource = new RandomGenSource();
		SampleGenerator sampleGenerator = SampleGenerator.from(ints, lists);
		sampleGenerator.filterOutDuplicates();
		for (int i = 0; i < 1000; i++) {
			List<GenSource> sources = List.of(randomGenSource.split(), randomGenSource.split());
			sampleGenerator.generate(sources).ifPresent(allSamples::add);
		}

		assertThat(allSamples).doesNotHaveDuplicates();
	}


	@Example
	void useRandomSampleGeneratorWithEdgeCases() {
		Generator<Integer> ints = new IntegerGenerator(-100, 100);
		Generator<Integer> withEdgeCases = WithEdgeCasesDecorator.decorate(ints, 0.9, 10);

		Set<Integer> values = new HashSet<>();
		SampleGenerator sampleGenerator = SampleGenerator.from(withEdgeCases);
		for (int i = 0; i < 100; i++) {
			RandomGenSource randomGenSource = new RandomGenSource();
			Sample sample = sampleGenerator.generate(List.of(randomGenSource)).orElseThrow();
			values.add((Integer) sample.values().getFirst());
		}
		assertThat(values).contains(
			0, 1, -1, 100, -100
		);
	}

	@Example
	void generateWithEdgeCases() {
		IntegerGenerator ints = new IntegerGenerator(-100, 100);
		Generator<Integer> intsWithEdgeCases = WithEdgeCasesDecorator.decorate(
			ints,
			0.5, 100
		);
		Generator<List<Integer>> lists =
			WithEdgeCasesDecorator.decorate(
				new ListGenerator<>(ints, 0, 5),
				0.5, 100
			);


		Set<List<Object>> values = new HashSet<>();
		SampleGenerator sampleGenerator = SampleGenerator.from(intsWithEdgeCases, lists);
		for (int i = 0; i < 100; i++) {
			RandomGenSource randomGenSource = new RandomGenSource();
			List<GenSource> sources = List.of(randomGenSource.split(), randomGenSource.split());
			Sample sample = sampleGenerator.generate(sources).orElseThrow();
			// System.out.println("sample = " + sample.values());

			values.add(sample.values());
			List<Object> regeneratedValues = sample.regenerateValues();
			assertThat(regeneratedValues).isEqualTo(sample.values());
		}
	}

}
