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
			Triple<SortedSet<FalsifiedSample>, Boolean, Optional<GuidedGeneration>> collectedRunResults = runAndCollectFalsifiedSamples(
				iterableGenSource, maxTries, sampleGenerator,
				countTries, countChecks,
				maxDuration, executorServiceSupplier
			);

			SortedSet<FalsifiedSample> falsifiedSamples = collectedRunResults.first();
			boolean timedOut = collectedRunResults.second();
			Optional<GuidedGeneration> guidedGeneration = collectedRunResults.third();

			PropertyRunResult runResult = createRunResult(
				countChecks, countTries, effectiveSeed, maxDuration,
				falsifiedSamples, shrinkingEnabled, timedOut
			);

			return guidedGeneration
					   .map(generation -> generation.overridePropertyResult(runResult))
					   .orElse(runResult);

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

	private Triple<SortedSet<FalsifiedSample>, Boolean, Optional<GuidedGeneration>> runAndCollectFalsifiedSamples(
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

		final Iterator<SampleSource> genSources = iterableGenSource.iterator();
		BiConsumer<TryExecutionResult, Sample> guide =
			(genSources instanceof GuidedGeneration)
				? ((GuidedGeneration) genSources)::guide
				: (result, sample) -> {};

		var taskIterator = new ConcurrentTaskIterator(
			genSources, maxTries,
			(trySource, shutdown) -> executeTry(
				sampleGenerator, trySource, countTries, countChecks,
				shutdown, iterableGenSource.stopWhenFalsified(),
				guide, onFalsified, onSatisfied, iterableGenSource.lock()
			)
		);

		Optional<GuidedGeneration> optionalGuidedGeneration =
			(genSources instanceof GuidedGeneration)
				? Optional.of((GuidedGeneration) genSources)
				: Optional.empty();

		try {
			runner.run(taskIterator);
			return new Triple<>(falsifiedSamples, false, optionalGuidedGeneration);
		} catch (TimeoutException timeoutException) {
			return new Triple<>(falsifiedSamples, true, optionalGuidedGeneration);
		}

	}

	private void executeTry(
		SampleGenerator sampleGenerator,
		SampleSource multiSource,
		AtomicInteger countTries,
		AtomicInteger countChecks,
		ConcurrentRunner.Shutdown shutdown, boolean stopWhenFalsified,
		BiConsumer<TryExecutionResult, Sample> guide, Consumer<FalsifiedSample> onFalsified, Consumer<Sample> onSatisfied,
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
			try {
				generationLock.lock();
				guide.accept(tryResult, sample);
			} finally {
				generationLock.unlock();
			}
			if (tryResult.status() != TryExecutionResult.Status.INVALID) {
				countChecks.incrementAndGet();
			}
			if (tryResult.status() == TryExecutionResult.Status.FALSIFIED) {
				FalsifiedSample originalSample = new FalsifiedSample(sample, tryResult.throwable());
				onFalsified.accept(originalSample);
				if (stopWhenFalsified) {
					shutdown.shutdown();
				}
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

	private static class ConcurrentTaskIterator implements Iterator<ConcurrentRunner.Task> {
		private final Iterator<SampleSource> genSources;
		private final int maxTries;
		private final BiConsumer<SampleSource, ConcurrentRunner.Shutdown> task;

		private int count = 0;
		private volatile boolean stopped = false;

		private ConcurrentTaskIterator(
			Iterator<SampleSource> genSources,
			int maxTries,
			BiConsumer<SampleSource, ConcurrentRunner.Shutdown> task
		) {
			this.genSources = genSources;
			this.maxTries = maxTries;
			this.task = task;
		}

		@Override
		public boolean hasNext() {
			if (stopped) {
				return false;
			}
			return genSources.hasNext() && count < maxTries;
		}

		@Override
		public ConcurrentRunner.Task next() {
			SampleSource trySource = genSources.next();
			count++;
			return shutdown -> {
				ConcurrentRunner.Shutdown shutdownAndStop = () -> {
					shutdown.shutdown();
					stopped = true;
				};
				task.accept(trySource, shutdownAndStop);
			};
		}

	}

}
