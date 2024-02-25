package jqwik2.api;

import java.time.*;
import java.util.*;
import java.util.function.*;

import jqwik2.api.recording.*;

public interface PropertyRunStrategy {

	interface Builder {
		PropertyRunStrategy build();

		Builder withMaxTries(int maxTries);

		Builder withMaxRuntime(Duration maxRuntime);

		Builder withFilterOutDuplicateSamples(boolean filterOutDuplicateSamples);

		Builder withGeneration(GenerationMode generation);

		Builder withAfterFailure(AfterFailureMode afterFailure);

		Builder withConcurrency(ConcurrencyMode concurrency);

		Builder withSeedSupplier(Supplier<String> seedSupplier);

		Builder withEdgeCases(EdgeCasesMode edgeCases);

		Builder withShrinking(ShrinkingMode shrinking);

		Builder withSamples(List<SampleRecording> samples);
	}

	PropertyRunStrategy DEFAULT = builder().build();

	static Builder builder() {
		return new StrategyBuilder();
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

class StrategyBuilder implements PropertyRunStrategy.Builder, Cloneable {

	private int maxTries = JqwikDefaults.defaultMaxTries();
	private Duration maxRuntime = JqwikDefaults.defaultMaxDuration();
	private boolean filterOutDuplicateSamples = JqwikDefaults.defaultFilterOutDuplicateSamples();
	private Supplier<String> seedSupplier = RandomChoice::generateRandomSeed;
	private List<SampleRecording> samples = List.of();
	private PropertyRunStrategy.ShrinkingMode shrinking = JqwikDefaults.defaultShrinkingMode();
	private PropertyRunStrategy.GenerationMode generation = JqwikDefaults.defaultGenerationMode();
	private PropertyRunStrategy.EdgeCasesMode edgeCases = JqwikDefaults.defaultEdgeCasesMode();
	private PropertyRunStrategy.AfterFailureMode afterFailure = JqwikDefaults.defaultAfterFailureMode();
	private PropertyRunStrategy.ConcurrencyMode concurrency = JqwikDefaults.defaultConcurrencyMode();

	@Override
	protected StrategyBuilder clone() {
		var strategyBuilder = new StrategyBuilder();
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
	public PropertyRunStrategy build() {
		return new DefaultStrategy(
			maxTries, maxRuntime, filterOutDuplicateSamples,
			seedSupplier, samples,
			shrinking, generation,
			edgeCases, afterFailure, concurrency
		);
	}

	@Override
	public PropertyRunStrategy.Builder withMaxTries(int maxTries) {
		StrategyBuilder clone = clone();
		clone.maxTries = maxTries;
		return clone;
	}

	@Override
	public PropertyRunStrategy.Builder withMaxRuntime(Duration maxRuntime) {
		StrategyBuilder clone = clone();
		clone.maxRuntime = maxRuntime;
		return clone;
	}

	@Override
	public PropertyRunStrategy.Builder withFilterOutDuplicateSamples(boolean filterOutDuplicateSamples) {
		StrategyBuilder clone = clone();
		clone.filterOutDuplicateSamples = filterOutDuplicateSamples;
		return clone;
	}

	@Override
	public PropertyRunStrategy.Builder withGeneration(PropertyRunStrategy.GenerationMode generation) {
		StrategyBuilder clone = clone();
		clone.generation = generation;
		return clone;
	}

	@Override
	public PropertyRunStrategy.Builder withAfterFailure(PropertyRunStrategy.AfterFailureMode afterFailure) {
		StrategyBuilder clone = clone();
		clone.afterFailure = afterFailure;
		return clone;
	}

	@Override
	public PropertyRunStrategy.Builder withConcurrency(PropertyRunStrategy.ConcurrencyMode concurrency) {
		StrategyBuilder clone = clone();
		clone.concurrency = concurrency;
		return clone;
	}

	@Override
	public PropertyRunStrategy.Builder withSeedSupplier(Supplier<String> seedSupplier) {
		StrategyBuilder clone = clone();
		clone.seedSupplier = seedSupplier;
		return clone;
	}

	@Override
	public PropertyRunStrategy.Builder withEdgeCases(PropertyRunStrategy.EdgeCasesMode edgeCases) {
		StrategyBuilder clone = clone();
		clone.edgeCases = edgeCases;
		return clone;
	}

	@Override
	public PropertyRunStrategy.Builder withShrinking(PropertyRunStrategy.ShrinkingMode shrinking) {
		StrategyBuilder clone = clone();
		clone.shrinking = shrinking;
		return clone;
	}

	@Override
	public PropertyRunStrategy.Builder withSamples(List<SampleRecording> samples) {
		StrategyBuilder clone = clone();
		clone.samples = samples;
		return clone;
	}
}