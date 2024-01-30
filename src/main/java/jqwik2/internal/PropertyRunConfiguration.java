package jqwik2.internal;

import java.time.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.function.*;

import jqwik2.api.*;
import jqwik2.api.recording.*;
import jqwik2.internal.exhaustive.*;

public interface PropertyRunConfiguration {

	Supplier<ExecutorService> DEFAULT_EXECUTOR_SERVICE_SUPPLIER = Executors::newSingleThreadExecutor;

	Duration maxRuntime();

	int maxTries();

	Optional<String> effectiveSeed();

	Supplier<ExecutorService> supplyExecutorService();

	boolean shrinkingEnabled();

	IterableSampleSource source();

	static PropertyRunConfiguration randomized(String seed, int maxTries) {
		return randomized(
			seed, maxTries, true,
			Duration.ofMinutes(10),
			DEFAULT_EXECUTOR_SERVICE_SUPPLIER
		);
	}

	static PropertyRunConfiguration randomized(
		String seed, int maxTries, boolean shrinkingEnabled
	) {
		return randomized(
			seed, maxTries,
			shrinkingEnabled,
			Duration.ofSeconds(10),
			Executors::newSingleThreadExecutor
		);
	}

	static PropertyRunConfiguration randomized(
		String seed, int maxTries, boolean shrinkingEnabled,
		Duration maxRuntime,
		Supplier<ExecutorService> supplyExecutorService
	) {
		return new RunConfigurationRecord(
			seed, maxTries,
			shrinkingEnabled,
			maxRuntime,
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
			supplyExecutorService,
			() -> new GuidedGenerationSource(guidanceSupplier)
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
			supplyExecutorService,
			() -> sampleSources
		);
	}

	static PropertyRunConfiguration smart(
		String seed, int maxTries, Duration maxRuntime,
		boolean shrinkingEnabled,
		Supplier<ExecutorService> supplyExecutorService,
		List<Generator<?>> generators
	) {
		var exhaustive = IterableExhaustiveSource.from(generators);
		if (exhaustive.isEmpty() || exhaustive.get().maxCount() > maxTries) {
			return randomized(seed, maxTries, shrinkingEnabled, maxRuntime, supplyExecutorService);
		}
		return new RunConfigurationRecord(
			null, maxTries,
			false,
			maxRuntime,
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
			defaultExecutorServiceSupplier,
			() -> sampleSource
		);
	}


}

