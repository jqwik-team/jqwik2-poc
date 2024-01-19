package jqwik2.api;

import java.time.*;
import java.util.*;

import jqwik2.api.recording.*;

public interface PropertyRunStrategy {

	PropertyRunStrategy DEFAULT = new DefaultStrategy(
		JqwikDefaults.defaultMaxTries(),
		JqwikDefaults.defaultMaxDuration(),
		Optional.of(RandomChoice.generateRandomSeed()),
		List.of(),
		JqwikDefaults.defaultShrinkingMode(),
		JqwikDefaults.defaultGenerationMode(),
		JqwikDefaults.defaultEdgeCasesMode()
	);

	static PropertyRunStrategy create(
		int maxTries, Duration maxRuntime, String seed,
		List<SampleRecording> samples,
		ShrinkingMode shrinking, GenerationMode generation, EdgeCasesMode edgeCases
	) {
		return new DefaultStrategy(maxTries, maxRuntime, Optional.ofNullable(seed), samples, shrinking, generation, edgeCases);
	}

	int maxTries();

	Duration maxRuntime();

	Optional<String> seed();

	ShrinkingMode shrinking();

	GenerationMode generation();

    EdgeCasesMode edgeCases();

	List<SampleRecording> samples();

	enum GenerationMode {
		RANDOMIZED,
		EXHAUSTIVE,
		SMART, // Use exhaustive if maxCount <= maxTries, otherwise use randomized,
		SAMPLES
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
	List<SampleRecording> samples,
	ShrinkingMode shrinking,
	GenerationMode generation,
	EdgeCasesMode edgeCases
) implements PropertyRunStrategy {}
