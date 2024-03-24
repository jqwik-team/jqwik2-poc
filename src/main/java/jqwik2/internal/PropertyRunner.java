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

import static jqwik2.api.validation.PropertyValidationStatus.*;

public class PropertyRunner {
	private final List<Generator<?>> generators;
	private final Tryable tryable;
	private final List<BiConsumer<TryExecutionResult,Sample>> tryExecutionListeners = new ArrayList<>();

	public PropertyRunner(List<Generator<?>> generators, Tryable tryable) {
		this.generators = generators;
		this.tryable = tryable;
	}

	/**
	 * Register a listener that will be called after each try execution.
	 * @param listener
	 */
	public void onTryExecution(BiConsumer<TryExecutionResult, Sample> listener) {
		if (listener == null || tryExecutionListeners.contains(listener)) {
			return;
		}
		tryExecutionListeners.add(listener);
	}

	private void sampleExecuted(TryExecutionResult result, Sample sample) {
		tryExecutionListeners.forEach(listener -> listener.accept(result, sample));
	}

	/**
	 * Run a property with one configuration after the other until one fails or all succeed.
	 *
	 * @param configurations list of configurations, at least one must be provided
	 * @return list of results, one for each started configuration
	 */
	public List<PropertyRunResult> run(List<PropertyRunConfiguration> configurations) {
		if (configurations.isEmpty()) {
			throw new IllegalArgumentException("At least one configuration must be provided");
		}
		List<PropertyRunResult> results = new ArrayList<>();
		for (PropertyRunConfiguration configuration : configurations) {
			var result = run(configuration);
			results.add(result);
			if (!result.isSuccessful()) {
				break;
			}
		}
		return results;
	}

	public PropertyRunResult run(PropertyRunConfiguration configuration) {
		IterableSampleSource iterableGenSource = randomSource(configuration);

		return runAndShrink(
			iterableGenSource,
			configuration.maxTries(), configuration.maxRuntime(),
			configuration.shrinkingEnabled(), configuration.filterOutDuplicateSamples(),
			configuration.executorService()
		);
	}

	@SuppressWarnings("OverlyLongMethod")
	private PropertyRunResult runAndShrink(
		IterableSampleSource iterableGenSource,
		int maxTries, Duration maxDuration,
		boolean shrinkingEnabled, boolean filterOutDuplicateSamples,
		Optional<ExecutorService> optionalExecutorService
	) {
		var genericGenerators = generators.stream().map(Generator::asGeneric).toList();
		SampleGenerator sampleGenerator = new SampleGenerator(genericGenerators);
		sampleGenerator.filterOutDuplicates(filterOutDuplicateSamples);

		AtomicInteger countTries = new AtomicInteger(0);
		AtomicInteger countChecks = new AtomicInteger(0);

		try {
			Triple<SortedSet<FalsifiedSample>, Boolean, Guidance> collectedRunResults = runAndCollectFalsifiedSamples(
				iterableGenSource, maxTries, sampleGenerator,
				countTries, countChecks,
				maxDuration, optionalExecutorService
			);

			SortedSet<FalsifiedSample> falsifiedSamples = collectedRunResults.first();
			boolean timedOut = collectedRunResults.second();
			Guidance guidance = collectedRunResults.third();

			PropertyRunResult runResult = createRunResult(
				countChecks, countTries, maxDuration,
				falsifiedSamples, shrinkingEnabled, timedOut
			);

			return runResult.withGuidance(guidance);

			// return guidance.overridePropertyResult(runResult);
		} catch (Throwable t) {
			ExceptionSupport.rethrowIfBlacklisted(t);
			return abortion(t, countTries, countChecks);
		}
	}

	private PropertyRunResult createRunResult(
		AtomicInteger countChecks, AtomicInteger countTries,
		Duration maxDuration, SortedSet<FalsifiedSample> falsifiedSamples, boolean shrinkingEnabled,
		boolean timedOut
	) {
		if (falsifiedSamples.isEmpty()) {
			if (hasTimedOutWithoutCheck(timedOut, countChecks)) {
				return timeOutAbortion(maxDuration, countTries, countChecks);
			}
			return success(countTries, countChecks, timedOut);
		} else {
			FalsifiedSample originalSample = falsifiedSamples.first();
			if (shrinkingEnabled) {
				shrink(originalSample, falsifiedSamples);
			}
			return failure(countTries, countChecks, falsifiedSamples, timedOut);
		}
	}

	private static PropertyRunResult abortion(
		Throwable t,
		AtomicInteger countTries,
		AtomicInteger countChecks
	) {
		return new PropertyRunResult(
			ABORTED, countTries.get(), countChecks.get(),
			new TreeSet<>(), Optional.empty(), Optional.of(t),
			false
		);
	}

	private static PropertyRunResult failure(
		AtomicInteger countTries,
		AtomicInteger countChecks,
		SortedSet<FalsifiedSample> falsifiedSamples,
		boolean timedOut
	) {
		return new PropertyRunResult(
			FAILED, countTries.get(), countChecks.get(),
			falsifiedSamples, Optional.empty(), Optional.empty(), timedOut
		);
	}

	private static PropertyRunResult success(
		AtomicInteger countTries,
		AtomicInteger countChecks,
		boolean timedOut
	) {
		return new PropertyRunResult(SUCCESSFUL, countTries.get(), countChecks.get(), timedOut);
	}

	private static PropertyRunResult timeOutAbortion(
		Duration maxDuration,
		AtomicInteger countTries,
		AtomicInteger countChecks
	) {
		String timedOutMessage = "Timeout after " + maxDuration + " without any check being executed.";
		Exception abortionReason = new TimeoutException(timedOutMessage);
		return new PropertyRunResult(
			ABORTED, countTries.get(), countChecks.get(),
			new TreeSet<>(), Optional.empty(), Optional.of(abortionReason),
			true
		);
	}

	private static boolean hasTimedOutWithoutCheck(boolean timedOut, AtomicInteger countChecks) {
		return timedOut && countChecks.get() < 1;
	}

	private Triple<SortedSet<FalsifiedSample>, Boolean, Guidance> runAndCollectFalsifiedSamples(
		IterableSampleSource iterableGenSource,
		int maxTries,
		SampleGenerator sampleGenerator,
		AtomicInteger countTries,
		AtomicInteger countChecks,
		Duration maxDuration,
		Optional<ExecutorService> optionalExecutorService
	) {
		SortedSet<FalsifiedSample> falsifiedSamples = Collections.synchronizedSortedSet(new TreeSet<>());
		Consumer<FalsifiedSample> onFalsified = falsifiedSamples::add;

		TaskRunner runner = optionalExecutorService
								.map(s -> (TaskRunner) new ConcurrentRunner(s, maxDuration))
								.orElseGet(() -> new InMainThreadRunner(maxDuration));

		final Iterator<SampleSource> genSources = iterableGenSource.iterator();

		Guidance guidance =
			(genSources instanceof Guidance g)
				? g
				: Guidance.NULL;

		var generationLock = iterableGenSource.lock();
		BiConsumer<TryExecutionResult, Sample> guide = (tryResult, sample) -> {
			try {
				generationLock.lock();
				guidance.guide(tryResult, sample);
			} finally {
				generationLock.unlock();
			}
		};

		var taskIterator = new ConcurrentTaskIterator(
			genSources, maxTries, sampleGenerator, countTries, guidance, generationLock,
			(sample, shutdown) -> executeTry(
				sample, countChecks,
				iterableGenSource.stopWhenFalsified(),
				onFalsified, guide, shutdown
			)
		);

		try {
			runner.run(taskIterator);
			return new Triple<>(falsifiedSamples, false, guidance);
		} catch (TimeoutException timeoutException) {
			return new Triple<>(falsifiedSamples, true, guidance);
		}

	}

	private void executeTry(
		Sample sample, AtomicInteger countChecks,
		boolean stopWhenFalsified,
		Consumer<FalsifiedSample> onFalsified,
		BiConsumer<TryExecutionResult, Sample> guide,
		TaskRunner.Shutdown shutdownAndStop
	) {
		TryExecutionResult tryResult = tryable.apply(sample);
		guide.accept(tryResult, sample);
		if (tryResult.status() != TryExecutionResult.Status.INVALID) {
			countChecks.incrementAndGet();
		}
		if (tryResult.status() == TryExecutionResult.Status.FALSIFIED) {
			FalsifiedSample originalSample = FalsifiedSample.original(sample, tryResult.throwable());
			onFalsified.accept(originalSample);
			if (stopWhenFalsified) {
				shutdownAndStop.shutdown();
			}
		}
		sampleExecuted(tryResult, sample);
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

	private static class ConcurrentTaskIterator implements Iterator<ConcurrentRunner.Task> {
		private final Iterator<SampleSource> genSources;
		private final int maxTries;
		private final SampleGenerator sampleGenerator;
		private final AtomicInteger countTries;
		private final Guidance guidance;
		private final Lock generationLock;
		private final BiConsumer<Sample, TaskRunner.Shutdown> task;

		private volatile boolean stopped = false;

		private ConcurrentTaskIterator(
			Iterator<SampleSource> genSources,
			int maxTries,
			SampleGenerator sampleGenerator,
			AtomicInteger countTries,
			Guidance guidance,
			Lock generationLock,
			BiConsumer<Sample, TaskRunner.Shutdown> task
		) {
			this.genSources = genSources;
			this.maxTries = maxTries;
			this.sampleGenerator = sampleGenerator;
			this.countTries = countTries;
			this.guidance = guidance;
			this.generationLock = generationLock;
			this.task = task;
		}

		@Override
		public boolean hasNext() {
			if (stopped) {
				return false;
			}
			return genSources.hasNext() && maxTriesNotReached();
		}

		private boolean maxTriesNotReached() {
			return maxTries == 0 || countTries.get() < maxTries;
		}

		@Override
		public ConcurrentRunner.Task next() {
			Optional<Sample> optionalSample;
			try {
				generationLock.lock();
				SampleSource trySource = genSources.next();
				optionalSample = sampleGenerator.generate(trySource);
				optionalSample.ifPresentOrElse(
					sample -> countTries.incrementAndGet(),
					() -> guidance.onEmptyGeneration(trySource)
				);
			} finally {
				generationLock.unlock();
			}

			return shutdown -> {
				TaskRunner.Shutdown shutdownAndStop = () -> {
					shutdown.shutdown();
					stopped = true;
					try {
						generationLock.lock();
						guidance.stop();
					} finally {
						generationLock.unlock();
					}
				};
				optionalSample.ifPresent(
					sample -> task.accept(sample, shutdownAndStop)
				);
			};
		}
	}

}
