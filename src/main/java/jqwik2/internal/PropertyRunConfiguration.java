package jqwik2.internal;

import java.time.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.function.*;

import jqwik2.api.*;
import jqwik2.api.recording.*;
import jqwik2.api.validation.*;
import jqwik2.internal.exhaustive.*;
import jqwik2.internal.growing.*;
import jqwik2.internal.reporting.*;

import static jqwik2.api.validation.PropertyValidationStrategy.GenerationMode.*;

public interface PropertyRunConfiguration {

	// null -> InMainThreadRunner is being used
	Supplier<ExecutorService> DEFAULT_EXECUTOR_SERVICE_SUPPLIER = null;

	Duration maxRuntime();

	int maxTries();

	Optional<String> effectiveSeed();

	Supplier<ExecutorService> supplyExecutorService();

	Optional<ExecutorService> executorService();

	boolean shrinkingEnabled();

	IterableSampleSource source();

	boolean filterOutDuplicateSamples();

	static PropertyRunConfiguration randomized(String seed, int maxTries) {
		return randomized(
			seed, maxTries, Duration.ofMinutes(10),
			true, false,
			DEFAULT_EXECUTOR_SERVICE_SUPPLIER
		);
	}

	static PropertyRunConfiguration randomized(
		String seed, int maxTries, boolean shrinkingEnabled, boolean filterOutDuplicateSamples
	) {
		return randomized(
			seed, maxTries, Duration.ofSeconds(10),
			shrinkingEnabled, filterOutDuplicateSamples,
			Executors::newSingleThreadExecutor
		);
	}

	static PropertyRunConfiguration randomized(
		String seed, int maxTries, Duration maxRuntime,
		boolean shrinkingEnabled, boolean filterOutDuplicateSamples,
		Supplier<ExecutorService> supplyExecutorService
	) {
		return new RunConfigurationRecord(
			seed, maxTries,
			maxRuntime, shrinkingEnabled,
			filterOutDuplicateSamples,
			supplyExecutorService,
			() -> randomSource(seed)
		);
	}

	static PropertyRunConfiguration guided(
		Supplier<GuidedGeneration> guidanceSupplier,
		int maxTries, Duration maxRuntime,
		boolean shrinkingEnabled, boolean filterOutDuplicateSamples,
		Supplier<ExecutorService> supplyExecutorService
	) {
		return new RunConfigurationRecord(
			null, maxTries, maxRuntime,
			shrinkingEnabled, filterOutDuplicateSamples,
			supplyExecutorService,
			() -> new GuidedGenerationSource(guidanceSupplier)
		);
	}

	static PropertyRunConfiguration wrapSource(
		PropertyRunConfiguration configuration,
		Function<IterableSampleSource, IterableSampleSource> sourceWrapper
	) {
		return new RunConfigurationRecord(
			configuration.effectiveSeed().orElse(null),
			configuration.maxTries(), configuration.maxRuntime(),
			configuration.shrinkingEnabled(), configuration.filterOutDuplicateSamples(),
			configuration.supplyExecutorService(),
			() -> sourceWrapper.apply(configuration.source())
		);
	}

	static PropertyRunConfiguration growing(
		int maxTries, boolean shrinkingEnabled, Duration maxRuntime
	) {
		return new RunConfigurationRecord(
			null, maxTries,
			maxRuntime, shrinkingEnabled,
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
			maxRuntime, false,
			false,
			supplyExecutorService,
			() -> sampleSources
		);
	}

	static PropertyRunConfiguration smart(
		String seed, int maxTries, Duration maxRuntime,
		boolean shrinkingEnabled, boolean filterOutDuplicateSamples,
		Supplier<ExecutorService> supplyExecutorService,
		List<Generator<?>> generators,
		ReportSection parametersReport
	) {
		var exhaustive = IterableExhaustiveSource.from(generators);
		if (exhaustive.isEmpty() || exhaustive.get().maxCount() > maxTries) {
			addSmartGenerationReport(RANDOMIZED, parametersReport);
			return randomized(
				seed, maxTries,
				maxRuntime, shrinkingEnabled, filterOutDuplicateSamples,
				supplyExecutorService
			);
		}
		addSmartGenerationReport(EXHAUSTIVE, parametersReport);
		return new RunConfigurationRecord(
			null, maxTries,
			maxRuntime, false,
			filterOutDuplicateSamples,
			supplyExecutorService,
			exhaustive::get
		);
	}

	private static void addSmartGenerationReport(PropertyValidationStrategy.GenerationMode randomized, ReportSection parametersReport) {
		var generationParameter = "%s (%s)".formatted(SMART.name(), randomized.name());
		parametersReport.append("generation", generationParameter);
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
			maxRuntime, shrinkingEnabled,
			false,
			defaultExecutorServiceSupplier,
			() -> sampleSource
		);
	}


}

