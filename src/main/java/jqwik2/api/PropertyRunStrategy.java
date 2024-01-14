package jqwik2.api;

import java.time.*;
import java.util.*;

public interface PropertyRunStrategy {

	PropertyRunStrategy DEFAULT = new DefaultStrategy(
		100, Duration.ofMinutes(10), Optional.of(RandomChoice.generateRandomSeed()),
		ShrinkingMode.FULL,
		GenerationMode.RANDOMIZED,
		EdgeCasesMode.MIXIN
	);

	static PropertyRunStrategy create(
		int maxTries, Duration maxRuntime, String seed,
		ShrinkingMode shrinking, GenerationMode generation, EdgeCasesMode edgeCases
	) {
		return new DefaultStrategy(maxTries, maxRuntime, Optional.ofNullable(seed), shrinking, generation, edgeCases);
	}

	int maxTries();

	Duration maxRuntime();

	Optional<String> seed();

	ShrinkingMode shrinking();

	GenerationMode generation();

    EdgeCasesMode edgeCases();

	enum GenerationMode {
		RANDOMIZED,
		EXHAUSTIVE,
		SMART // Use exhaustive if maxCount <= maxTries, otherwise use randomized
	}

	enum ShrinkingMode {
		OFF,
		FULL
	}

	enum EdgeCasesMode {
		OFF, // Do not generate edge cases explicitly
		MIXIN // Mix edge cases into random generation
	}

}

record DefaultStrategy(
	int maxTries, Duration maxRuntime,
	Optional<String> seed,
	ShrinkingMode shrinking,
	GenerationMode generation,
	EdgeCasesMode edgeCases
) implements PropertyRunStrategy {}
