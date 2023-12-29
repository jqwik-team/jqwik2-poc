package jqwik2;

import java.util.*;
import java.util.stream.*;

import jqwik2.api.*;

public class SampleGenerator {

	public static SampleGenerator from(Generator<?>... generators) {
		return new SampleGenerator(toObjectGenerators(List.of(generators)));
	}

	private final List<Generator<Object>> generators;

	public SampleGenerator(List<Generator<Object>> generators) {
		this.generators = generators;
	}

	public Sample generate(List<? extends GenSource> genSources) {
		if (genSources.size() != generators.size()) {
			throw new IllegalArgumentException("Number of gen sources must match number of generators");
		}
		List<Shrinkable<Object>> shrinkables = new ArrayList<>();
		for (int i = 0; i < generators.size(); i++) {
			Generator<Object> generator = generators.get(i);
			shrinkables.add(createShrinkable(genSources.get(i), generator));
		}
		return new Sample(shrinkables);
	}

	private static List<Generator<Object>> toObjectGenerators(List<Generator<?>> generators) {
		return generators.stream()
						 .map(Generator::asGeneric)
						 .toList();
	}

	private static Shrinkable<Object> createShrinkable(GenSource source, Generator<Object> generator) {
		return new ShrinkableGenerator<>(generator).generate(source);
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
