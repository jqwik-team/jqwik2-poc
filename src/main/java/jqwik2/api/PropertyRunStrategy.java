package jqwik2.api;

import java.time.*;
import java.util.*;
import java.util.function.*;

import jqwik2.api.recording.*;

public interface PropertyRunStrategy {

	PropertyRunStrategy DEFAULT = new DefaultStrategy(
		JqwikDefaults.defaultMaxTries(),
		JqwikDefaults.defaultMaxDuration(),
		RandomChoice::generateRandomSeed,
		List.of(),
		JqwikDefaults.defaultShrinkingMode(),
		JqwikDefaults.defaultGenerationMode(),
		JqwikDefaults.defaultEdgeCasesMode(),
		JqwikDefaults.defaultAfterFailureMode()
	);

	static PropertyRunStrategy create(
		int maxTries, Duration maxRuntime, Supplier<String> seed,
		List<SampleRecording> samples,
		ShrinkingMode shrinking, GenerationMode generation, EdgeCasesMode edgeCases, AfterFailureMode afterFailure
	) {
		return new DefaultStrategy(
			maxTries, maxRuntime,
			seed, samples,
			shrinking, generation, edgeCases, afterFailure
		);
	}

	int maxTries();

	Duration maxRuntime();

	Supplier<String> seedSupplier();

	List<SampleRecording> samples();

	ShrinkingMode shrinking();

	GenerationMode generation();

    EdgeCasesMode edgeCases();

    AfterFailureMode afterFailure();

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

	enum AfterFailureMode {
		REPLAY,
		SAMPLES_ONLY
	}
}

record DefaultStrategy(
	int maxTries, Duration maxRuntime,
	Supplier<String> seedSupplier,
	List<SampleRecording> samples,
	ShrinkingMode shrinking,
	GenerationMode generation,
	EdgeCasesMode edgeCases,
	AfterFailureMode afterFailure
) implements PropertyRunStrategy {}
