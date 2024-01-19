package jqwik2.internal;

import java.util.*;

import jqwik2.api.*;

public class SampleGenerator {

	public static SampleGenerator from(Generator<?>... generators) {
		return new SampleGenerator(toObjectGenerators(List.of(generators)));
	}

	private final List<Generator<Object>> generators;

	public SampleGenerator(List<Generator<Object>> generators) {
		this.generators = generators;
	}

	public Optional<Sample> generate(SampleSource multiSource) {
		List<GenSource> genSources = multiSource.sources(generators.size());
		List<Shrinkable<Object>> shrinkables = new ArrayList<>();
		try {
			for (int i = 0; i < generators.size(); i++) {
				Generator<Object> generator = generators.get(i);
				shrinkables.add(createShrinkable(genSources.get(i), generator));
			}
		} catch (CannotGenerateException cge) {
			return Optional.empty();
		}
		return Optional.of(new Sample(shrinkables));
	}

	private static List<Generator<Object>> toObjectGenerators(List<Generator<?>> generators) {
		return generators.stream()
						 .map(Generator::asGeneric)
						 .toList();
	}

	private static Shrinkable<Object> createShrinkable(GenSource source, Generator<Object> generator) {
		return new ShrinkableGenerator<>(generator).generate(source);
	}

	public Optional<Sample> generate(List<GenSource> sources) {
		return generate(SampleSource.of(sources));
	}

}
