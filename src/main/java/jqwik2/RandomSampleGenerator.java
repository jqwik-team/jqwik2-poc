package jqwik2;

import java.util.*;

public class RandomSampleGenerator {

	private final List<Generator<Object>> generators;

	public RandomSampleGenerator(List<Generator<?>> generators) {
		this.generators = generators.stream()
									.map(gen -> (Generator<Object>) gen)
									.toList();
	}

	public Sample generate(RandomGenSource randomGenSource) {
		List<Shrinkable<Object>> shrinkables =
			generators.stream()
					  .map(gen -> {
						  GenRecorder recorder = new GenRecorder(randomGenSource.split());
						  Object value = gen.generate(recorder);
						  return (Shrinkable<Object>) new GeneratedShrinkable<>(value, gen, recorder.recording());
					  })
					  .toList();
		return new Sample(shrinkables);
	}
}
