package jqwik2;

import java.util.*;

public record RandomSample(
	List<Shrinkable<Object>> shrinkables,
	String randomSeed
) implements Sample {
	static RandomSample create(List<Generator<?>> generators, String randomSeed) {
		var random = new RandomGenSource(Long.parseLong(randomSeed));
		var shrinkables = generators.stream()
									.map(g -> g.generate(random))
									.map(Shrinkable::asGeneric)
									.toList();
		return new RandomSample(shrinkables, randomSeed);
	}

	@Override
	public Sample regenerate() {
		List<Generator<?>> generators = new ArrayList<>();
		for (Shrinkable<Object> shrinkable : shrinkables) {
			generators.add(shrinkable.generator());
		}
		return create(generators, randomSeed);
	}
}
