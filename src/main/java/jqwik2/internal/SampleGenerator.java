package jqwik2.internal;

import java.util.*;
import java.util.concurrent.atomic.*;

import jqwik2.api.*;

public class SampleGenerator {

	private final static int MAX_DUPLICATE_SAMPLES = 1000;

	public static SampleGenerator from(Generator<?>... generators) {
		return new SampleGenerator(toObjectGenerators(List.of(generators)));
	}

	private final List<Generator<Object>> generators;
	private final Set<Sample> generatedSamples = new HashSet<>();
	private final AtomicInteger countDuplicates = new AtomicInteger(0);
	private boolean filterOutDuplicates = false;

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
			var sample = new Sample(shrinkables);
			if (filterOutDuplicates) {
				if (generatedSamples.contains(sample)) {
					countDuplicates.incrementAndGet();
					if (countDuplicates.get() > MAX_DUPLICATE_SAMPLES) {
						throw new CannotGenerateException("Too many (%s) duplicate samples".formatted(MAX_DUPLICATE_SAMPLES));
					}
					return Optional.empty();
				}
				generatedSamples.add(sample);
			}
			return Optional.of(sample);
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

	public void filterOutDuplicates(boolean filterOutDuplicates) {
		this.filterOutDuplicates = filterOutDuplicates;
	}

	public void filterOutDuplicates() {
		filterOutDuplicates(true);
	}
}
