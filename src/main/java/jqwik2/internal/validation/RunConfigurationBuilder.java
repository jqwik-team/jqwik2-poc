package jqwik2.internal.validation;

import java.util.*;
import java.util.concurrent.*;
import java.util.function.*;

import jqwik2.api.*;
import jqwik2.api.database.*;
import jqwik2.api.recording.*;
import jqwik2.api.validation.*;
import jqwik2.internal.*;
import jqwik2.internal.growing.*;

import static jqwik2.api.validation.PropertyValidationStrategy.GenerationMode.*;

class RunConfigurationBuilder {

	private final PropertyValidationStrategy strategy;
	private final String id;
	private final FailureDatabase database;
	private final List<Generator<?>> generators;

	RunConfigurationBuilder(String id, List<Generator<?>> generators, PropertyValidationStrategy strategy, FailureDatabase database) {
		this.id = id;
		this.strategy = strategy;
		this.database = database;
		this.generators = generators;
	}

	PropertyRunConfiguration build(Reporter reporter) {
		if (database.hasFailed(id)) {
			reporter.appendToReport(Reporter.CATEGORY_PARAMETER, "after failure", strategy.afterFailure().name());
			return switch (strategy.afterFailure()) {
				case REPLAY -> replayLastRun(generators, reporter);
				case SAMPLES_ONLY -> {
					List<SampleRecording> samples = new ArrayList<>(database.loadFailingSamples(id));
					if (samples.isEmpty()) {
						yield replayLastRun(generators, reporter);
					}
					Collections.sort(samples); // Sorts from smallest to largest
					yield PropertyRunConfiguration.samples(
						strategy.maxRuntime(),
						isShrinkingEnabled(),
						samples,
						serviceSupplier()
					);
				}
				case null -> throw new IllegalStateException("Property has failed before: " + id);
			};
		}
		return buildDefaultConfiguration(generators, strategy.seedSupplier(), reporter);
	}

	private PropertyRunConfiguration replayLastRun(List<Generator<?>> generators, Reporter reporter) {
		Supplier<String> seedSupplier = database.loadSeed(id)
												.map(s -> (Supplier<String>) () -> s)
												.orElseGet(strategy::seedSupplier);
		return buildDefaultConfiguration(generators, seedSupplier, reporter);
	}

	private PropertyRunConfiguration buildDefaultConfiguration(
		List<Generator<?>> generators,
		Supplier<String> seedSupplier,
		Reporter reporter
	) {
		return switch (strategy.generation()) {
			case RANDOMIZED -> {
				reporter.appendToReport(Reporter.CATEGORY_PARAMETER, "generation", RANDOMIZED.name());
				yield PropertyRunConfiguration.randomized(
					seedSupplier.get(),
					strategy.maxTries(),
					strategy.maxRuntime(), isShrinkingEnabled(),
					strategy.filterOutDuplicateSamples(),
					serviceSupplier()
				);
			}
			case EXHAUSTIVE -> {
				reporter.appendToReport(Reporter.CATEGORY_PARAMETER, "generation", EXHAUSTIVE.name());
				yield PropertyRunConfiguration.exhaustive(
					strategy.maxTries(),
					strategy.maxRuntime(),
					serviceSupplier(),
					generators
				);
			}
			case SMART -> PropertyRunConfiguration.smart(
				seedSupplier.get(),
				strategy.maxTries(),
				strategy.maxRuntime(),
				isShrinkingEnabled(),
				strategy.filterOutDuplicateSamples(),
				serviceSupplier(),
				generators,
				reporter
			);
			case SAMPLES -> {
				reporter.appendToReport(Reporter.CATEGORY_PARAMETER, "generation", EXHAUSTIVE.name());
				yield PropertyRunConfiguration.samples(
					strategy.maxRuntime(),
					isShrinkingEnabled(),
					strategy.samples(),
					serviceSupplier()
				);
			}
			case GROWING -> {
				reporter.appendToReport(Reporter.CATEGORY_PARAMETER, "generation", EXHAUSTIVE.name());
				yield PropertyRunConfiguration.guided(
					GrowingSampleSource::new,
					strategy.maxTries(), strategy.maxRuntime(),
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
		return strategy.shrinking() == PropertyValidationStrategy.ShrinkingMode.FULL;
	}
}
