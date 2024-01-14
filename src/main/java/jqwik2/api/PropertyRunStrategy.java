package jqwik2.api;

import java.time.*;
import java.util.*;

public interface PropertyRunStrategy {

	PropertyRunStrategy DEFAULT = new DefaultStrategy(
		100, Duration.ofMinutes(10), Optional.of(RandomChoice.generateRandomSeed()),
		Shrinking.FULL,
		Generation.RANDOMIZED,
		EdgeCases.MIXIN
	);

	static PropertyRunStrategy create(
		int maxTries, Duration maxRuntime, String seed,
		Shrinking shrinking, Generation generation, EdgeCases edgeCases
	) {
		return new DefaultStrategy(maxTries, maxRuntime, Optional.ofNullable(seed), shrinking, generation, edgeCases);
	}

	int maxTries();

	Duration maxRuntime();

	Optional<String> seed();

	Shrinking shrinking();

	Generation generation();

    EdgeCases edgeCases();

	enum Generation {
		RANDOMIZED,
		EXHAUSTIVE,
		SMART // Use exhaustive if maxCount <= maxTries, otherwise use randomized
	}

	enum Shrinking {
		OFF,
		FULL
	}

	enum EdgeCases {
		OFF, // Do not generate edge cases explicitly
		MIXIN // Mix edge cases into random generation
	}

}

record DefaultStrategy(
	int maxTries, Duration maxRuntime,
	Optional<String> seed,
	Shrinking shrinking,
	Generation generation,
	EdgeCases edgeCases
) implements PropertyRunStrategy {}
