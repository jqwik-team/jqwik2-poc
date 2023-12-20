package jqwik2;

import java.util.*;

import net.jqwik.api.*;

import static org.assertj.core.api.Assertions.*;

class SampleTests {

	@Example
	void generateRandomSample() {
		Generator<Integer> ints = new IntegerGenerator(-100, 100);
		Generator<List<Integer>> lists = new ListGenerator<>(ints, 5);

		GenRecorder recorder1 = new GenRecorder(new RandomGenSource(41l));
		GenRecorder recorder2 = new GenRecorder(new RandomGenSource(42l));

		List<Shrinkable<Object>> shrinkables = List.of(
			ints.generate(recorder1).asGeneric(),
			lists.generate(recorder2).asGeneric()
		);

		Sample sample = new Sample(shrinkables);

		System.out.println("sample = " + sample);

		List<Object> regeneratedValues = sample.regenerateValues();
		assertThat(regeneratedValues).isEqualTo(sample.values());
	}
}
