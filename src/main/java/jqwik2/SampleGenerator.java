package jqwik2;

import java.util.*;
import java.util.function.*;
import java.util.stream.*;

import jqwik2.api.*;
import jqwik2.api.recording.*;

public class SampleGenerator {

	private final List<Generator<Object>> generators;
	private final double edgeCasesProbability;
	private final int maxEdgeCases;

	public SampleGenerator(List<Generator<?>> generators) {
		this(generators, 0.0, 100);
	}

	public SampleGenerator(List<Generator<?>> generators, double edgeCasesProbability, int maxEdgeCases) {
		this.generators = toObjectGenerators(generators);
		this.edgeCasesProbability = edgeCasesProbability;
		this.maxEdgeCases = maxEdgeCases;
	}

	@SuppressWarnings("unchecked")
	private static List<Generator<Object>> toObjectGenerators(List<Generator<?>> generators) {
		return generators.stream()
						 .map(gen -> (Generator<Object>) gen)
						 .toList();
	}

	public Sample generate(List<? extends GenSource> genSources) {
		if (genSources.size() != generators.size()) {
			throw new IllegalArgumentException("Number of gen sources must match number of generators");
		}
		List<Shrinkable<Object>> shrinkables = new ArrayList<>();
		for (int i = 0; i < generators.size(); i++) {
			Generator<Object> generator = decorateWithEdgeCases(generators.get(i));
			// GenSource randomOrEdgeCaseSource = chooseRandomOrEdgeCaseSource(genSources.get(i), generator);
			// shrinkables.add(createShrinkable(randomOrEdgeCaseSource, generator));
			shrinkables.add(createShrinkable(genSources.get(i), generator));
		}
		return new Sample(shrinkables);
	}

	private static Shrinkable<Object> createShrinkable(GenSource source, Generator<Object> generator) {
		return new ShrinkableGenerator<>(generator).generate(source);
	}

	private Generator<Object> decorateWithEdgeCases(Generator<Object> generator) {
		if (edgeCasesProbability <= 0.0) {
			return generator;
		}
		return generator.decorate(g -> new WithEdgeCasesDecorator(g, edgeCasesProbability, maxEdgeCases));
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
