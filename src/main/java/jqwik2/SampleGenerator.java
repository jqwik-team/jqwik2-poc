package jqwik2;

import java.util.*;
import java.util.stream.*;

import jqwik2.api.*;

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
			shrinkables.add(createShrinkable(source, generator));
		}
		return new Sample(shrinkables);
	}

	private static Shrinkable<Object> createShrinkable(GenSource source, Generator<Object> generator) {
		GenRecorder recorder = new GenRecorder(source);
		return new ShrinkableGenerator<>(generator).generate(recorder);
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
