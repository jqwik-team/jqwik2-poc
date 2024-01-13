package jqwik2.api;

import java.time.*;
import java.util.*;

public interface PropertyRunStrategy {

	PropertyRunStrategy DEFAULT = new DefaultStrategy(
		100, Duration.ofMinutes(10), Optional.of(RandomChoice.generateRandomSeed()),
		Shrinking.FULL,
		Generation.RANDOMIZED
	);

	int maxTries();

	Duration maxRuntime();

	Optional<String> seed();

	Shrinking shrinking();

	Generation generation();

	enum Generation {
		RANDOMIZED,
		EXHAUSTIVE
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
