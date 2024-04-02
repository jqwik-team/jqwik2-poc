package jqwik2.internal.validation;

import java.time.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.function.*;

import jqwik2.api.*;
import jqwik2.api.database.*;
import jqwik2.api.recording.*;
import jqwik2.api.validation.*;
import jqwik2.api.validation.PropertyValidationStrategy.*;
import jqwik2.internal.*;
import jqwik2.internal.growing.*;
import jqwik2.internal.reporting.*;

import static jqwik2.api.validation.PropertyValidationStrategy.GenerationMode.*;

class RunConfigurationBuilder {

	private final PropertyValidationStrategy strategy;
	private final String id;
	private final FailureDatabase database;
	private final List<Generator<?>> generators;
	private int maxTries;
	private Duration maxRuntime;
	private ShrinkingMode shrinking;
	private EdgeCasesMode edgeCases;
	private ConcurrencyMode concurrency;
	private boolean filterOutDuplicateSamples;

	RunConfigurationBuilder(String id, List<Generator<?>> generators, PropertyValidationStrategy strategy, FailureDatabase database) {
		this.id = id;
		this.strategy = strategy;
		this.database = database;
		this.generators = generators;
		this.maxTries = strategy.maxTries();
		this.maxRuntime = strategy.maxRuntime();
		this.shrinking = strategy.shrinking();
		this.edgeCases = strategy.edgeCases();
		this.concurrency = strategy.concurrency();
		this.filterOutDuplicateSamples = strategy.filterOutDuplicateSamples();
	}

	PropertyRunConfiguration build(ReportSection parametersReport) {
		PropertyRunConfiguration runConfiguration = buildConfiguration(parametersReport);
		parametersReport.append("max tries", runConfiguration.maxTries());
		parametersReport.append("max runtime", runConfiguration.maxRuntime());
		parametersReport.append("filter duplicates", filterOutDuplicateSamples);
		parametersReport.append("shrinking", shrinking);
		parametersReport.append("edge cases", edgeCases);
		parametersReport.append("concurrency", concurrency);
		runConfiguration.effectiveSeed().ifPresent(
			seed -> parametersReport.append("seed", seed)
		);

		return runConfiguration;
	}

	private PropertyRunConfiguration buildConfiguration(ReportSection parametersReport) {
		if (database.hasFailed(id)) {
			return buildAfterFailureConfiguration(parametersReport);
		}
		return buildDefaultConfiguration(generators, strategy.seedSupplier(), parametersReport);
	}

	private PropertyRunConfiguration buildAfterFailureConfiguration(ReportSection parametersReport) {
		parametersReport.append("after failure", strategy.afterFailure().name());
		return switch (strategy.afterFailure()) {
			case REPLAY -> replayLastRun(generators, parametersReport);
			case SAMPLES_ONLY -> {
				List<SampleRecording> samples = new ArrayList<>(database.loadFailingSamples(id));
				if (samples.isEmpty()) {
					yield replayLastRun(generators, parametersReport);
				}
				Collections.sort(samples); // Sorts from smallest to largest
				yield PropertyRunConfiguration.samples(
					maxRuntime,
					isShrinkingEnabled(),
					samples,
					serviceSupplier()
				);
			}
			case null -> throw new IllegalStateException("Property has failed before: " + id);
		};
	}

	private PropertyRunConfiguration replayLastRun(List<Generator<?>> generators, ReportSection parametersReport) {
		Supplier<String> seedSupplier = database.loadSeed(id)
												.map(s -> (Supplier<String>) () -> s)
												.orElseGet(strategy::seedSupplier);
		return buildDefaultConfiguration(generators, seedSupplier, parametersReport);
	}

	private PropertyRunConfiguration buildDefaultConfiguration(
		List<Generator<?>> generators,
		Supplier<String> seedSupplier,
		ReportSection parametersReport
	) {
		return switch (strategy.generation()) {
			case RANDOMIZED -> {
				parametersReport.append("generation", RANDOMIZED.name());
				yield randomizedConfiguration(seedSupplier);
			}
			case EXHAUSTIVE -> {
				parametersReport.append("generation", EXHAUSTIVE.name());
				yield exhaustiveConfiguration(generators);
			}
			case SMART -> smartConfiguration(generators, seedSupplier, parametersReport);
			case SAMPLES -> {
				parametersReport.append("generation", SAMPLES.name());
				yield samplesConfiguration();
			}
			case GROWING -> {
				parametersReport.append("generation", GROWING.name());
				yield growingConfiguration();
			}
			case null -> throw new IllegalArgumentException("Unsupported generation strategy: " + strategy.generation());
		};
	}

	private PropertyRunConfiguration growingConfiguration() {
		this.shrinking = ShrinkingMode.OFF;
		this.concurrency = ConcurrencyMode.SINGLE_THREAD;
		this.filterOutDuplicateSamples = false;
		this.edgeCases = EdgeCasesMode.OFF;
		return PropertyRunConfiguration.record(
			null, maxTries,
			maxRuntime, isShrinkingEnabled(),
			filterOutDuplicateSamples,
			serviceSupplier(),
			IterableGrowingSource::new
		);
	}

	private PropertyRunConfiguration samplesConfiguration() {
		return PropertyRunConfiguration.samples(
			maxRuntime,
			isShrinkingEnabled(),
			strategy.samples(),
			serviceSupplier()
		);
	}

	private PropertyRunConfiguration smartConfiguration(
		List<Generator<?>> generators,
		Supplier<String> seedSupplier,
		ReportSection parametersReport
	) {
		return PropertyRunConfiguration.smart(
			seedSupplier.get(),
			maxTries,
			maxRuntime,
			isShrinkingEnabled(),
			filterOutDuplicateSamples,
			serviceSupplier(),
			generators,
			parametersReport
		);
	}

	private PropertyRunConfiguration exhaustiveConfiguration(List<Generator<?>> generators) {
		return PropertyRunConfiguration.exhaustive(
			maxTries,
			maxRuntime,
			serviceSupplier(),
			generators
		);
	}

	private PropertyRunConfiguration randomizedConfiguration(Supplier<String> seedSupplier) {
		return PropertyRunConfiguration.randomized(
			seedSupplier.get(),
			maxTries,
			maxRuntime, isShrinkingEnabled(),
			filterOutDuplicateSamples,
			serviceSupplier()
		);
	}

	private Supplier<ExecutorService> serviceSupplier() {
		return switch (concurrency) {
			case SINGLE_THREAD -> null;
			case CACHED_THREAD_POOL -> Executors::newCachedThreadPool;
			case FIXED_THREAD_POOL -> () -> Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());
			case VIRTUAL_THREADS -> Executors::newVirtualThreadPerTaskExecutor;
			case null -> throw new IllegalArgumentException("Unsupported concurrency mode: " + concurrency);
		};
	}

	private boolean isShrinkingEnabled() {
		return shrinking == ShrinkingMode.FULL;
	}

	RunConfigurationBuilder forStatisticalCheck() {
		this.maxTries = Integer.MAX_VALUE;
		this.maxRuntime = Duration.ZERO;
		this.shrinking = ShrinkingMode.OFF;
		return this;
	}
}
