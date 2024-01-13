package jqwik2.api;

import java.time.*;
import java.util.*;

public interface PropertyRunStrategy {

	PropertyRunStrategy DEFAULT = new DefaultStrategy(
		100, Duration.ofMinutes(10), Optional.of(RandomChoice.generateRandomSeed()),
		Shrinking.FULL,
		Generation.RANDOMIZED
	);

	static PropertyRunStrategy create(int maxTries, Duration maxRuntime, String seed, Shrinking shrinking, Generation generation) {
		return new DefaultStrategy(maxTries, maxRuntime, Optional.ofNullable(seed), shrinking, generation);
	}

	int maxTries();

	Duration maxRuntime();

	Optional<String> seed();

	Shrinking shrinking();

	Generation generation();

	enum Generation {
		RANDOMIZED,
		EXHAUSTIVE,
		SMART_EXHAUSTIVE
	}

	enum Shrinking {
		OFF,
		FULL
	}

}

record DefaultStrategy(
	int maxTries, Duration maxRuntime,
	Optional<String> seed,
	Shrinking shrinking,
	Generation generation
) implements PropertyRunStrategy {}
