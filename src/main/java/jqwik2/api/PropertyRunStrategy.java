package jqwik2.api;

import java.time.*;
import java.util.*;
import java.util.function.*;

import jqwik2.api.recording.*;

public interface PropertyRunStrategy {

	PropertyRunStrategy DEFAULT = new DefaultStrategy(
		JqwikDefaults.defaultMaxTries(),
		JqwikDefaults.defaultMaxDuration(),
		JqwikDefaults.defaultFilterOutDuplicateSamples(),
		RandomChoice::generateRandomSeed,
		List.of(),
		JqwikDefaults.defaultShrinkingMode(),
		JqwikDefaults.defaultGenerationMode(),
		JqwikDefaults.defaultEdgeCasesMode(),
		JqwikDefaults.defaultAfterFailureMode(),
		JqwikDefaults.defaultConcurrencyMode()
	);

	static PropertyRunStrategy create(
		int maxTries, Duration maxRuntime, boolean filterOutDuplicateSamples,
		Supplier<String> seed, List<SampleRecording> samples,
		ShrinkingMode shrinking, GenerationMode generation,
		EdgeCasesMode edgeCases, AfterFailureMode afterFailure, ConcurrencyMode concurrency
	) {
		return new DefaultStrategy(
			maxTries, maxRuntime, filterOutDuplicateSamples,
			seed, samples,
			shrinking, generation,
			edgeCases, afterFailure, concurrency
		);
	}

	int maxTries();

	Duration maxRuntime();

	boolean filterOutDuplicateSamples();

	Supplier<String> seedSupplier();

	List<SampleRecording> samples();

	ShrinkingMode shrinking();

	GenerationMode generation();

    EdgeCasesMode edgeCases();

    AfterFailureMode afterFailure();

	ConcurrencyMode concurrency();

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

	enum ConcurrencyMode {
		SINGLE_THREAD,
		CACHED_THREAD_POOL,
		FIXED_THREAD_POOL,
		VIRTUAL_THREADS
	}

}

record DefaultStrategy(
	int maxTries, Duration maxRuntime,
	boolean filterOutDuplicateSamples,
	Supplier<String> seedSupplier,
	List<SampleRecording> samples,
	ShrinkingMode shrinking,
	GenerationMode generation,
	EdgeCasesMode edgeCases,
	AfterFailureMode afterFailure,
	ConcurrencyMode concurrency
) implements PropertyRunStrategy {}
