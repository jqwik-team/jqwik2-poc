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

	public PropertyRunResult run(PropertyRunConfiguration configuration) {
		IterableSampleSource iterableGenSource = randomSource(configuration);
		int maxTries = configuration.maxTries();
		boolean shrinkingEnabled = configuration.shrinkingEnabled();

		return runAndShrink(
			iterableGenSource,
			maxTries, configuration.effectiveSeed(),
			shrinkingEnabled,
			configuration.maxRuntime(),
			configuration.supplyExecutorService()
		);
	}

	@SuppressWarnings("OverlyLongMethod")
	private PropertyRunResult runAndShrink(
		IterableSampleSource iterableGenSource,
		int maxTries, Optional<String> effectiveSeed,
		boolean shrinkingEnabled,
		Duration maxDuration,
		Supplier<ExecutorService> executorServiceSupplier
	) {
		var genericGenerators = generators.stream().map(Generator::asGeneric).toList();
		SampleGenerator sampleGenerator = new SampleGenerator(genericGenerators);

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
						ABORTED, countTries.get(), countChecks.get(), effectiveSeed,
						new TreeSet<>(), Optional.of(abortionReason),
						timedOut
					);
				}
				return new PropertyRunResult(SUCCESSFUL, countTries.get(), countChecks.get(), effectiveSeed, timedOut);
			}

			FalsifiedSample originalSample = falsifiedSamples.first();
			if (shrinkingEnabled) {
				shrink(originalSample, falsifiedSamples);
			}

			return new PropertyRunResult(
				FAILED, countTries.get(), countChecks.get(), effectiveSeed,
				falsifiedSamples, Optional.empty(), timedOut
			);

		} catch (Throwable t) {
			ExceptionSupport.rethrowIfBlacklisted(t);
			return new PropertyRunResult(
				ABORTED, countTries.get(), countChecks.get(), effectiveSeed,
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
		Optional<Sample> optionalSample;
		try {
			generationLock.lock();
			optionalSample = sampleGenerator.generate(multiSource);
		} finally {
			generationLock.unlock();
		}
		optionalSample.ifPresent(sample -> {
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
		});
	}

	private static IterableSampleSource randomSource(PropertyRunConfiguration configuration) {
		return configuration.source();
	}

	private void shrink(FalsifiedSample originalSample, Collection<FalsifiedSample> falsifiedSamples) {
		FalsifiedSample best = new FullShrinker(originalSample, tryable).shrinkToEnd(ignore -> {});
		falsifiedSamples.add(best);

		// TODO: Should all shrunk examples be recorded?
		// new FullShrinker(originalSample, tryable).shrinkToEnd(
		// 	falsifiedSamples::add
		// );
	}

}
