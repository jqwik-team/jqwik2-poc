package jqwik2;

import java.util.*;
import java.util.stream.*;

public class SampleGenerator {

	private final List<Generator<Object>> generators;

	@SuppressWarnings("unchecked")
	public SampleGenerator(List<Generator<?>> generators) {
		this.generators = generators.stream()
									.map(gen -> (Generator<Object>) gen)
									.toList();
	}

	public Sample generate(List<? extends GenSource> genSources) {
		if (genSources.size() != generators.size()) {
			throw new IllegalArgumentException("Number of gen sources must match number of generators");
		}
		List<Shrinkable<Object>> shrinkables = new ArrayList<>();
		for (int i = 0; i < generators.size(); i++) {
			Generator<Object> generator = generators.get(i);
			GenSource source = genSources.get(i);
			GenRecorder recorder = new GenRecorder(source);
			Object value = generator.generate(recorder);
			Shrinkable<Object> apply = new GeneratedShrinkable<>(value, generator, recorder.recording());
			shrinkables.add(apply);
		}
		return new Sample(shrinkables);
	}

	public Sample generate(GenSource... sources) {
		return generate(List.of(sources));
	}

	public Sample generateRandomly(RandomGenSource randomGenSource) {
		List<RandomGenSource> genSources =
			IntStream.range(0, generators.size())
					 .mapToObj(i -> randomGenSource.split())
					 .toList();
		return generate(genSources);
	}
}
