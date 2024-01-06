package jqwik2.internal;

import java.time.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;
import java.util.concurrent.locks.*;
import java.util.function.*;

import jqwik2.api.*;
import jqwik2.api.support.*;
import jqwik2.internal.shrinking.*;

import static jqwik2.api.PropertyRunResult.Status.*;

public class PropertyCase {
	private final List<Generator<?>> generators;
	private final Tryable tryable;

	private Consumer<Sample> onSatisfied = ignore -> {};

	public PropertyCase(List<Generator<?>> generators, Tryable tryable) {
		this.generators = generators;
		this.tryable = tryable;
	}

	public void onSuccessful(Consumer<Sample> onSuccessful) {
		this.onSatisfied = onSuccessful;
	}

	public PropertyRunResult run(PropertyRunConfiguration runConfiguration) {
		return switch (runConfiguration) {
			case PropertyRunConfiguration.Randomized randomized -> runRandomized(randomized);
			case null, default -> throw new IllegalArgumentException("Unsupported run configuration: " + runConfiguration);
		};
	}

	private PropertyRunResult runRandomized(PropertyRunConfiguration.Randomized randomized) {
		IterableSampleSource iterableGenSource = randomSource(randomized);
		int maxTries = randomized.maxTries();
		boolean shrinkingEnabled = randomized.shrinkingEnabled();

		int maxEdgeCases = Math.max(maxTries, 10);
		double edgeCasesProbability = randomized.edgeCasesProbability();
		List<Generator<Object>> effectiveGenerators = withEdgeCases(edgeCasesProbability, maxEdgeCases);

		return runAndShrink(
			effectiveGenerators,
			iterableGenSource,
			maxTries,
			shrinkingEnabled,
			randomized.maxRuntime(),
			randomized.supplyExecutorService()
		);
	}

	@SuppressWarnings("OverlyLongMethod")
	private PropertyRunResult runAndShrink(
		List<Generator<Object>> effectiveGenerators,
		IterableSampleSource iterableGenSource,
		int maxTries,
		boolean shrinkingEnabled,
		Duration maxDuration,
		Supplier<ExecutorService> executorServiceSupplier
	) {
		SampleGenerator sampleGenerator = new SampleGenerator(effectiveGenerators);

		AtomicInteger countTries = new AtomicInteger(0);
		AtomicInteger countChecks = new AtomicInteger(0);

		try {
			Pair<SortedSet<FalsifiedSample>, Boolean> falsifiedAndTimedOut = runAndCollectFalsifiedSamples(
				iterableGenSource, maxTries, sampleGenerator,
				countTries, countChecks,
				maxDuration, executorServiceSupplier
			);

			SortedSet<FalsifiedSample> falsifiedSamples = falsifiedAndTimedOut.first();
			boolean timedOut = falsifiedAndTimedOut.second();

			if (falsifiedSamples.isEmpty()) {
				if (timedOut && countChecks.get() < 1) {
					String timedOutMessage = "Timeout after " + maxDuration + " without any check being executed.";
					Exception abortionReason = new TimeoutException(timedOutMessage);
					return new PropertyRunResult(
						ABORTED, countTries.get(), countChecks.get(),
						new TreeSet<>(), Optional.of(abortionReason),
						timedOut
					);
				}
				return new PropertyRunResult(SUCCESSFUL, countTries.get(), countChecks.get(), timedOut);
			}

			FalsifiedSample originalSample = falsifiedSamples.first();
			if (shrinkingEnabled) {
				shrink(originalSample, falsifiedSamples);
			}

			return new PropertyRunResult(
				FAILED, countTries.get(), countChecks.get(),
				falsifiedSamples, Optional.empty(), timedOut
			);

		} catch (Throwable t) {
			ExceptionSupport.rethrowIfBlacklisted(t);
			return new PropertyRunResult(
				ABORTED, countTries.get(), countChecks.get(),
				new TreeSet<>(), Optional.of(t),
				false
			);
		}
	}

	private Pair<SortedSet<FalsifiedSample>, Boolean> runAndCollectFalsifiedSamples(
		IterableSampleSource iterableGenSource,
		int maxTries,
		SampleGenerator sampleGenerator,
		AtomicInteger countTries,
		AtomicInteger countChecks,
		Duration maxDuration,
		Supplier<ExecutorService> executorServiceSupplier
	) {
		SortedSet<FalsifiedSample> falsifiedSamples = Collections.synchronizedSortedSet(new TreeSet<>());
		Consumer<FalsifiedSample> onFalsified = falsifiedSamples::add;

		var runner = new ConcurrentRunner(executorServiceSupplier.get(), maxDuration);

		var taskIterator = new Iterator<ConcurrentRunner.Task>() {
			private final Iterator<SampleSource> genSources = iterableGenSource.iterator();
			private int count = 0;

			@Override
			public boolean hasNext() {
				return genSources.hasNext() && count < maxTries;
			}

			@Override
			public ConcurrentRunner.Task next() {
				SampleSource trySource = genSources.next();
				count++;
				return shutdown -> executeTry(
					sampleGenerator, trySource, countTries, countChecks,
					shutdown, onFalsified, onSatisfied, iterableGenSource.lock()
				);
			}
		};

		try {
			runner.run(taskIterator);
			return new Pair<>(falsifiedSamples, false);
		} catch (TimeoutException timeoutException) {
			return new Pair<>(falsifiedSamples, true);
		}

	}

	private void executeTry(
		SampleGenerator sampleGenerator,
		SampleSource multiSource,
		AtomicInteger countTries,
		AtomicInteger countChecks,
		ConcurrentRunner.Shutdown shutdown,
		Consumer<FalsifiedSample> onFalsified,
		Consumer<Sample> onSatisfied,
		Lock generationLock
	) {
		try {
			generationLock.lock();
			Sample sample = sampleGenerator.generate(multiSource);
			generationLock.unlock();
			countTries.incrementAndGet();
			TryExecutionResult tryResult = tryable.apply(sample);
			if (tryResult.status() != TryExecutionResult.Status.INVALID) {
				countChecks.incrementAndGet();
			}
			if (tryResult.status() == TryExecutionResult.Status.FALSIFIED) {
				FalsifiedSample originalSample = new FalsifiedSample(sample, tryResult.throwable());
				onFalsified.accept(originalSample);
				shutdown.shutdown();
			}
			if (tryResult.status() == TryExecutionResult.Status.SATISFIED) {
				onSatisfied.accept(sample);
			}
		} finally {
			generationLock.unlock();
		}
	}

	private List<Generator<Object>> withEdgeCases(double edgeCasesProbability, int maxEdgeCases) {
		return generators.stream()
						 .map(gen -> decorateWithEdgeCases(gen.asGeneric(), edgeCasesProbability, maxEdgeCases))
						 .toList();
	}

	private static Generator<Object> decorateWithEdgeCases(Generator<Object> generator, double edgeCasesProbability, int maxEdgeCases) {
		if (edgeCasesProbability <= 0.0) {
			return generator;
		}
		return WithEdgeCasesDecorator.decorate(generator, edgeCasesProbability, maxEdgeCases);
	}

	private static IterableSampleSource randomSource(PropertyRunConfiguration.Randomized randomized) {
		return randomized.seed() == null
				   ? new RandomGenSource()
				   : new RandomGenSource(randomized.seed());
	}

	private void shrink(FalsifiedSample originalSample, Collection<FalsifiedSample> falsifiedSamples) {
		new FullShrinker(originalSample, tryable).shrinkToEnd(
			falsifiedSamples::add
		);
	}

}
