package jqwik2;

import java.util.*;
import java.util.stream.*;

import jqwik2.api.*;
import jqwik2.api.recording.*;

public class SampleGenerator {

	private final List<Generator<Object>> generators;
	private final double edgeCasesProbability;
	private final int maxEdgeCases;

	private final Map<Generator<Object>, Collection<Recording>> edgeCasesCache = new HashMap<>();

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
			Generator<Object> generator = generators.get(i);
			GenSource randomOrEdgeCaseSource = chooseRandomOrEdgeCaseSource(genSources.get(i), generator);
			shrinkables.add(createShrinkable(randomOrEdgeCaseSource, generator));
		}
		return new Sample(shrinkables);
	}

	private static Shrinkable<Object> createShrinkable(GenSource source, Generator<Object> generator) {
		GenRecorder recorder = new GenRecorder(source);
		return new ShrinkableGenerator<>(generator).generate(recorder);
	}

	private GenSource chooseRandomOrEdgeCaseSource(GenSource originalSource, Generator<Object> generator) {
		if (originalSource instanceof RandomGenSource randomSource) {
			return randomSource.withProbability(
				edgeCasesProbability,
				() -> edgeCaseSource(generator, randomSource),
				() -> originalSource
			);
		}
		return originalSource;
	}

	private RecordedSource edgeCaseSource(Generator<Object> generator, RandomGenSource randomSource) {
		Collection<Recording> edgeCases = edgeCasesCache.computeIfAbsent(
			generator,
			this::createEdgeCaseRecordings
		);
		Recording recording = randomSource.chooseOne(edgeCases);
		return new RecordedSource(recording);
	}

	private Collection<Recording> createEdgeCaseRecordings(Generator<Object> generator) {

		LinkedHashSet<Recording> edgeCases = new LinkedHashSet<>();
		Iterator<Recording> iterator = generator.edgeCases().iterator();
		while (edgeCases.size() < maxEdgeCases) {
			if (!iterator.hasNext()) {
				break;
			}
			edgeCases.add(iterator.next());
		}
		return edgeCases;
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
