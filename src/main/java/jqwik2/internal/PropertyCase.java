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

		return runAndShrink(
			iterableGenSource,
			configuration.effectiveSeed(), configuration.maxTries(), configuration.maxRuntime(),
			configuration.shrinkingEnabled(), configuration.filterOutDuplicateSamples(),
			configuration.executorService()
		);
	}

	@SuppressWarnings("OverlyLongMethod")
	private PropertyRunResult runAndShrink(
		IterableSampleSource iterableGenSource,
		Optional<String> effectiveSeed, int maxTries, Duration maxDuration,
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
				countChecks, countTries, effectiveSeed, maxDuration,
				falsifiedSamples, shrinkingEnabled, timedOut
			);

			return guidance.overridePropertyResult(runResult);
		} catch (Throwable t) {
			ExceptionSupport.rethrowIfBlacklisted(t);
			return abortion(effectiveSeed, t, countTries, countChecks);
		}
	}

	private PropertyRunResult createRunResult(
		AtomicInteger countChecks, AtomicInteger countTries, Optional<String> effectiveSeed,
		Duration maxDuration, SortedSet<FalsifiedSample> falsifiedSamples, boolean shrinkingEnabled,
		boolean timedOut
	) {
		if (falsifiedSamples.isEmpty()) {
			if (hasTimedOutWithoutCheck(timedOut, countChecks)) {
				return timeOutAbortion(effectiveSeed, maxDuration, countTries, countChecks);
			}
			return success(effectiveSeed, countTries, countChecks, timedOut);
		} else {
			FalsifiedSample originalSample = falsifiedSamples.first();
			if (shrinkingEnabled) {
				shrink(originalSample, falsifiedSamples);
			}
			return failure(effectiveSeed, countTries, countChecks, falsifiedSamples, timedOut);
		}
	}

	private static PropertyRunResult abortion(
		Optional<String> effectiveSeed,
		Throwable t,
		AtomicInteger countTries,
		AtomicInteger countChecks
	) {
		return new PropertyRunResult(
			ABORTED, countTries.get(), countChecks.get(), effectiveSeed,
			new TreeSet<>(), Optional.empty(), Optional.of(t),
			false
		);
	}

	private static PropertyRunResult failure(
		Optional<String> effectiveSeed,
		AtomicInteger countTries,
		AtomicInteger countChecks,
		SortedSet<FalsifiedSample> falsifiedSamples,
		boolean timedOut
	) {
		return new PropertyRunResult(
			FAILED, countTries.get(), countChecks.get(), effectiveSeed,
			falsifiedSamples, Optional.empty(), Optional.empty(), timedOut
		);
	}

	private static PropertyRunResult success(
		Optional<String> effectiveSeed,
		AtomicInteger countTries,
		AtomicInteger countChecks,
		boolean timedOut
	) {
		return new PropertyRunResult(SUCCESSFUL, countTries.get(), countChecks.get(), effectiveSeed, timedOut);
	}

	private static PropertyRunResult timeOutAbortion(
		Optional<String> effectiveSeed,
		Duration maxDuration,
		AtomicInteger countTries,
		AtomicInteger countChecks
	) {
		String timedOutMessage = "Timeout after " + maxDuration + " without any check being executed.";
		Exception abortionReason = new TimeoutException(timedOutMessage);
		return new PropertyRunResult(
			ABORTED, countTries.get(), countChecks.get(), effectiveSeed,
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
				onFalsified, onSatisfied, guide, shutdown
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
		Consumer<Sample> onSatisfied,
		BiConsumer<TryExecutionResult, Sample> guide,
		TaskRunner.Shutdown shutdownAndStop
	) {
		TryExecutionResult tryResult = tryable.apply(sample);
		guide.accept(tryResult, sample);
		if (tryResult.status() != TryExecutionResult.Status.INVALID) {
			countChecks.incrementAndGet();
		}
		if (tryResult.status() == TryExecutionResult.Status.FALSIFIED) {
			FalsifiedSample originalSample = new FalsifiedSample(sample, tryResult.throwable());
			onFalsified.accept(originalSample);
			if (stopWhenFalsified) {
				shutdownAndStop.shutdown();
			}
		}
		if (tryResult.status() == TryExecutionResult.Status.SATISFIED) {
			onSatisfied.accept(sample);
		}
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

		private volatile int count = 0;
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
			return maxTries == 0 || count < maxTries;
		}

		@Override
		public ConcurrentRunner.Task next() {
			// TODO: Move generation and try counting out of task
			//       Only count try if a sample has been generated
			SampleSource trySource = genSources.next();
			count++;
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

				Optional<Sample> optionalSample;
				try {
					generationLock.lock();
					optionalSample = sampleGenerator.generate(trySource);
					optionalSample.ifPresentOrElse(
						sample -> {
							countTries.incrementAndGet();
							task.accept(sample, shutdownAndStop);
						},
						() -> guidance.onEmptyGeneration(trySource)
					);
				} finally {
					generationLock.unlock();
				}
			};
		}

	}

}
