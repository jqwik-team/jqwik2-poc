package jqwik2.api.validation;

import java.time.*;
import java.util.*;
import java.util.function.*;

import jqwik2.api.*;
import jqwik2.api.recording.*;

public interface PropertyValidationStrategy {

	interface Builder {
		PropertyValidationStrategy build();

		Builder withMaxTries(int maxTries);

		Builder withMaxRuntime(Duration maxRuntime);

		Builder withFilterOutDuplicateSamples(boolean filterOutDuplicateSamples);

		Builder withGeneration(GenerationMode generation);

		Builder withAfterFailure(AfterFailureMode afterFailure);

		Builder withConcurrency(ConcurrencyMode concurrency);

		Builder withSeedSupplier(Supplier<String> seedSupplier);

		default Builder withSeed(String seed) {
			return withSeedSupplier(() -> seed);
		}

		Builder withEdgeCases(EdgeCasesMode edgeCases);

		Builder withShrinking(ShrinkingMode shrinking);

		Builder withSamples(List<SampleRecording> samples);
	}

	PropertyValidationStrategy DEFAULT = builder().build();

	static Builder builder() {
		return new ValidationStrategyBuilder();
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
		SAMPLES,
		GROWING
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

record DefaultValidationStrategy(
	int maxTries, Duration maxRuntime,
	boolean filterOutDuplicateSamples,
	Supplier<String> seedSupplier,
	List<SampleRecording> samples,
	ShrinkingMode shrinking,
	GenerationMode generation,
	EdgeCasesMode edgeCases,
	AfterFailureMode afterFailure,
	ConcurrencyMode concurrency
) implements PropertyValidationStrategy {}

class ValidationStrategyBuilder implements PropertyValidationStrategy.Builder, Cloneable {

	private int maxTries = JqwikDefaults.defaultMaxTries();
	private Duration maxRuntime = JqwikDefaults.defaultMaxDuration();
	private boolean filterOutDuplicateSamples = JqwikDefaults.defaultFilterOutDuplicateSamples();
	private Supplier<String> seedSupplier = RandomChoice::generateRandomSeed;
	private List<SampleRecording> samples = List.of();
	private PropertyValidationStrategy.ShrinkingMode shrinking = JqwikDefaults.defaultShrinkingMode();
	private PropertyValidationStrategy.GenerationMode generation = JqwikDefaults.defaultGenerationMode();
	private PropertyValidationStrategy.EdgeCasesMode edgeCases = JqwikDefaults.defaultEdgeCasesMode();
	private PropertyValidationStrategy.AfterFailureMode afterFailure = JqwikDefaults.defaultAfterFailureMode();
	private PropertyValidationStrategy.ConcurrencyMode concurrency = JqwikDefaults.defaultConcurrencyMode();

	@Override
	protected ValidationStrategyBuilder clone() {
		var strategyBuilder = new ValidationStrategyBuilder();
		strategyBuilder.maxTries = maxTries;
		strategyBuilder.maxRuntime = maxRuntime;
		strategyBuilder.filterOutDuplicateSamples = filterOutDuplicateSamples;
		strategyBuilder.seedSupplier = seedSupplier;
		strategyBuilder.samples = samples;
		strategyBuilder.shrinking = shrinking;
		strategyBuilder.generation = generation;
		strategyBuilder.edgeCases = edgeCases;
		strategyBuilder.afterFailure = afterFailure;
		strategyBuilder.concurrency = concurrency;
		return strategyBuilder;
	}

	@Override
	public PropertyValidationStrategy build() {
		return new DefaultValidationStrategy(
			maxTries, maxRuntime, filterOutDuplicateSamples,
			seedSupplier, samples,
			shrinking, generation,
			edgeCases, afterFailure, concurrency
		);
	}

	@Override
	public PropertyValidationStrategy.Builder withMaxTries(int maxTries) {
		ValidationStrategyBuilder clone = clone();
		clone.maxTries = maxTries;
		return clone;
	}

	@Override
	public PropertyValidationStrategy.Builder withMaxRuntime(Duration maxRuntime) {
		ValidationStrategyBuilder clone = clone();
		clone.maxRuntime = maxRuntime;
		return clone;
	}

	@Override
	public PropertyValidationStrategy.Builder withFilterOutDuplicateSamples(boolean filterOutDuplicateSamples) {
		ValidationStrategyBuilder clone = clone();
		clone.filterOutDuplicateSamples = filterOutDuplicateSamples;
		return clone;
	}

	@Override
	public PropertyValidationStrategy.Builder withGeneration(PropertyValidationStrategy.GenerationMode generation) {
		ValidationStrategyBuilder clone = clone();
		clone.generation = generation;
		return clone;
	}

	@Override
	public PropertyValidationStrategy.Builder withAfterFailure(PropertyValidationStrategy.AfterFailureMode afterFailure) {
		ValidationStrategyBuilder clone = clone();
		clone.afterFailure = afterFailure;
		return clone;
	}

	@Override
	public PropertyValidationStrategy.Builder withConcurrency(PropertyValidationStrategy.ConcurrencyMode concurrency) {
		ValidationStrategyBuilder clone = clone();
		clone.concurrency = concurrency;
		return clone;
	}

	@Override
	public PropertyValidationStrategy.Builder withSeedSupplier(Supplier<String> seedSupplier) {
		ValidationStrategyBuilder clone = clone();
		clone.seedSupplier = seedSupplier;
		return clone;
	}

	@Override
	public PropertyValidationStrategy.Builder withEdgeCases(PropertyValidationStrategy.EdgeCasesMode edgeCases) {
		ValidationStrategyBuilder clone = clone();
		clone.edgeCases = edgeCases;
		return clone;
	}

	@Override
	public PropertyValidationStrategy.Builder withShrinking(PropertyValidationStrategy.ShrinkingMode shrinking) {
		ValidationStrategyBuilder clone = clone();
		clone.shrinking = shrinking;
		return clone;
	}

	@Override
	public PropertyValidationStrategy.Builder withSamples(List<SampleRecording> samples) {
		ValidationStrategyBuilder clone = clone();
		clone.samples = samples;
		return clone;
	}
}