package jqwik2.internal.validation;

import java.time.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.function.*;

import jqwik2.api.*;
import jqwik2.api.database.*;
import jqwik2.api.recording.*;
import jqwik2.api.validation.*;
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
	private PropertyValidationStrategy.ShrinkingMode shrinking;

	RunConfigurationBuilder(String id, List<Generator<?>> generators, PropertyValidationStrategy strategy, FailureDatabase database) {
		this.id = id;
		this.strategy = strategy;
		this.database = database;
		this.generators = generators;
		this.maxTries = strategy.maxTries();
		this.maxRuntime = strategy.maxRuntime();
		this.shrinking = strategy.shrinking();
	}

	PropertyRunConfiguration build(ReportSection parametersReport) {
		if (database.hasFailed(id)) {
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
		return buildDefaultConfiguration(generators, strategy.seedSupplier(), parametersReport);
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
				yield PropertyRunConfiguration.randomized(
					seedSupplier.get(),
					maxTries,
					maxRuntime, isShrinkingEnabled(),
					strategy.filterOutDuplicateSamples(),
					serviceSupplier()
				);
			}
			case EXHAUSTIVE -> {
				parametersReport.append("generation", EXHAUSTIVE.name());
				yield PropertyRunConfiguration.exhaustive(
					maxTries,
					maxRuntime,
					serviceSupplier(),
					generators
				);
			}
			case SMART -> PropertyRunConfiguration.smart(
				seedSupplier.get(),
				maxTries,
				maxRuntime,
				isShrinkingEnabled(),
				strategy.filterOutDuplicateSamples(),
				serviceSupplier(),
				generators,
				parametersReport
			);
			case SAMPLES -> {
				parametersReport.append("generation", SAMPLES.name());
				yield PropertyRunConfiguration.samples(
					maxRuntime,
					isShrinkingEnabled(),
					strategy.samples(),
					serviceSupplier()
				);
			}
			case GROWING -> {
				parametersReport.append("generation", GROWING.name());
				yield PropertyRunConfiguration.guided(
					GrowingSampleSource::new,
					maxTries, maxRuntime,
					false, true,
					serviceSupplier()
				);
			}
			case null -> throw new IllegalArgumentException("Unsupported generation strategy: " + strategy.generation());
		};
	}

	private Supplier<ExecutorService> serviceSupplier() {
		return switch (strategy.concurrency()) {
			case SINGLE_THREAD -> null;
			case CACHED_THREAD_POOL -> Executors::newCachedThreadPool;
			case FIXED_THREAD_POOL -> () -> Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());
			case VIRTUAL_THREADS -> Executors::newVirtualThreadPerTaskExecutor;
			case null -> throw new IllegalArgumentException("Unsupported concurrency mode: " + strategy.concurrency());
		};
	}

	private boolean isShrinkingEnabled() {
		return shrinking == PropertyValidationStrategy.ShrinkingMode.FULL;
	}

	RunConfigurationBuilder forStatisticalCheck() {
		this.maxTries = Integer.MAX_VALUE;
		this.maxRuntime = Duration.ZERO;
		this.shrinking = PropertyValidationStrategy.ShrinkingMode.OFF;
		return this;
	}
}
