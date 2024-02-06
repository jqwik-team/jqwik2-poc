package jqwik2.internal;

import java.time.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.function.*;

import jqwik2.api.*;
import jqwik2.api.recording.*;
import jqwik2.internal.exhaustive.*;
import jqwik2.internal.growing.*;

public interface PropertyRunConfiguration {

	// null -> InMainThreadRunner is being used
	Supplier<ExecutorService> DEFAULT_EXECUTOR_SERVICE_SUPPLIER = null;

	Duration maxRuntime();

	int maxTries();

	Optional<String> effectiveSeed();

	Optional<ExecutorService> executorService();

	boolean shrinkingEnabled();

	IterableSampleSource source();

	boolean filterOutDuplicateSamples();

	static PropertyRunConfiguration randomized(String seed, int maxTries) {
		return randomized(
			seed, maxTries, true,
			Duration.ofMinutes(10),
			false,
			DEFAULT_EXECUTOR_SERVICE_SUPPLIER
		);
	}

	static PropertyRunConfiguration randomized(
		String seed, int maxTries, boolean shrinkingEnabled, boolean filterOutDuplicateSamples
	) {
		return randomized(
			seed, maxTries,
			shrinkingEnabled,
			Duration.ofSeconds(10),
			filterOutDuplicateSamples,
			Executors::newSingleThreadExecutor
		);
	}

	static PropertyRunConfiguration randomized(
		String seed, int maxTries, boolean shrinkingEnabled,
		Duration maxRuntime, boolean filterOutDuplicateSamples,
		Supplier<ExecutorService> supplyExecutorService
	) {
		return new RunConfigurationRecord(
			seed, maxTries,
			shrinkingEnabled,
			maxRuntime,
			filterOutDuplicateSamples,
			supplyExecutorService,
			() -> randomSource(seed)
		);
	}

	static PropertyRunConfiguration guided(
		Supplier<GuidedGeneration> guidanceSupplier,
		int maxTries, boolean shrinkingEnabled,
		Duration maxRuntime,
		Supplier<ExecutorService> supplyExecutorService
	) {
		return new RunConfigurationRecord(
			null,
			maxTries,
			shrinkingEnabled,
			maxRuntime,
			false,
			supplyExecutorService,
			() -> new GuidedGenerationSource(guidanceSupplier)
		);
	}

	static PropertyRunConfiguration growing(
		int maxTries, boolean shrinkingEnabled, Duration maxRuntime
	) {
		return new RunConfigurationRecord(
			null, maxTries,
			shrinkingEnabled,
			maxRuntime,
			true,
			Executors::newSingleThreadExecutor,
			IterableGrowingSource::new
		);
	}


	private static IterableSampleSource randomSource(String seed) {
		return seed == null
				   ? new RandomGenSource()
				   : new RandomGenSource(seed);
	}

	static PropertyRunConfiguration exhaustive(
		int maxTries, Duration maxRuntime,
		Supplier<ExecutorService> supplyExecutorService,
		List<Generator<?>> generators
	) {
		var sampleSources =
			IterableExhaustiveSource.from(generators)
									.orElseThrow(() -> {
										var message = "Exhaustive generation is not possible for given generators";
										return new IllegalArgumentException(message);
									});
		return new RunConfigurationRecord(
			null, maxTries,
			false,
			maxRuntime,
			false,
			supplyExecutorService,
			() -> sampleSources
		);
	}

	static PropertyRunConfiguration smart(
		String seed, int maxTries, Duration maxRuntime,
		boolean shrinkingEnabled, boolean filterOutDuplicateSamples,
		Supplier<ExecutorService> supplyExecutorService,
		List<Generator<?>> generators
	) {
		var exhaustive = IterableExhaustiveSource.from(generators);
		if (exhaustive.isEmpty() || exhaustive.get().maxCount() > maxTries) {
			return randomized(
				seed, maxTries,
				shrinkingEnabled, maxRuntime, filterOutDuplicateSamples,
				supplyExecutorService
			);
		}
		return new RunConfigurationRecord(
			null, maxTries,
			false,
			maxRuntime,
			filterOutDuplicateSamples,
			supplyExecutorService,
			exhaustive::get
		);
	}

	static PropertyRunConfiguration samples(
		Duration maxRuntime,
		boolean shrinkingEnabled,
		List<SampleRecording> samples,
		Supplier<ExecutorService> defaultExecutorServiceSupplier
	) {
		IterableSampleSource sampleSource = new RecordedSamplesSource(samples);
		return new RunConfigurationRecord(
			null, samples.size(),
			shrinkingEnabled,
			maxRuntime,
			false,
			defaultExecutorServiceSupplier,
			() -> sampleSource
		);
	}


}

