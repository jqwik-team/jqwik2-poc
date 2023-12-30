package jqwik2;

import java.time.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;
import java.util.function.*;

import jqwik2.api.*;
import jqwik2.api.support.*;

import static jqwik2.api.PropertyRunResult.Status.*;

public class PropertyCase {
	private final List<Generator<?>> generators;
	private final Tryable tryable;

	private Consumer<Sample> onSatisfied = ignore -> {};

	public PropertyCase(List<Generator<?>> generators, Tryable tryable) {
		this.generators = generators;
		this.tryable = tryable;
	}

	void onSuccessful(Consumer<Sample> onSuccessful) {
		this.onSatisfied = onSuccessful;
	}

	public PropertyRunResult run(PropertyRunConfiguration runConfiguration) {
		return switch (runConfiguration) {
			case PropertyRunConfiguration.Randomized randomized -> runRandomized(randomized);
			case null, default -> throw new IllegalArgumentException("Unsupported run configuration: " + runConfiguration);
		};
	}

	private PropertyRunResult runRandomized(PropertyRunConfiguration.Randomized randomized) {
		IterableGenSource iterableGenSource = randomSource(randomized);
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

	private PropertyRunResult runAndShrink(
		List<Generator<Object>> effectiveGenerators,
		IterableGenSource iterableGenSource,
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
					String abortionReason = "Timeout after " + maxDuration + " without any check being executed.";
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
				new TreeSet<>(), Optional.of(t.getMessage()),
				false
			);
		}
	}

	@SuppressWarnings("OverlyLongMethod")
	private Pair<SortedSet<FalsifiedSample>, Boolean> runAndCollectFalsifiedSamples(
		IterableGenSource iterableGenSource,
		int maxTries,
		SampleGenerator sampleGenerator,
		AtomicInteger countTries,
		AtomicInteger countChecks,
		Duration maxDuration,
		Supplier<ExecutorService> executorServiceSupplier
	) {
		int count = 0;
		SortedSet<FalsifiedSample> falsifiedSamples = Collections.synchronizedSortedSet(new TreeSet<>());
		Set<Throwable> thrownErrors = Collections.synchronizedSet(new HashSet<>());
		Iterator<MultiGenSource> genSources = iterableGenSource.iterator();
		try (var executorService = executorServiceSupplier.get()) {
			Consumer<FalsifiedSample> onFalsified = sample -> {
				falsifiedSamples.add(sample);
				executorService.shutdownNow();
			};
			Consumer<Throwable> onError = throwable -> {
				executorService.shutdownNow();
				thrownErrors.add(throwable);
			};

			while (count < maxTries) {
				if (!genSources.hasNext()) {
					break;
				}
				count++;
				List<GenSource> tryGenSources = genSources.next().sources(generators.size());
				try {
					Runnable executeTry = () -> executeTry(
						sampleGenerator, tryGenSources, countTries, countChecks,
						onFalsified, onSatisfied, onError
					);
					executorService.submit(executeTry);
				} catch (RejectedExecutionException ignore) {
					// This can happen when a task is submitted after
					// the executor service has been shut down due to a falsified sample.
				}
			}
			boolean timedOut = waitForAllTriesToFinishOrAtLeastOneIsFalsified(executorService, maxDuration, countChecks);
			if (!thrownErrors.isEmpty()) {
				ExceptionSupport.throwAsUnchecked(thrownErrors.iterator().next());
			}
			return new Pair<>(falsifiedSamples, timedOut);
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

	private static IterableGenSource randomSource(PropertyRunConfiguration.Randomized randomized) {
		return randomized.seed() == null
				   ? new RandomGenSource()
				   : new RandomGenSource(randomized.seed());
	}

	private boolean waitForAllTriesToFinishOrAtLeastOneIsFalsified(
		ExecutorService executorService, Duration timeout,
		AtomicInteger countChecks
	) {
		boolean timeoutOccurred = false;
		try {
			executorService.shutdown();
			timeoutOccurred = !executorService.awaitTermination(timeout.toMillis(), TimeUnit.MILLISECONDS);
		} catch (InterruptedException e) {
			executorService.shutdownNow();
		}
		if (timeoutOccurred) {
			executorService.shutdownNow();
			return true;
		} else {
			return false;
		}
	}

	private void executeTry(
		SampleGenerator sampleGenerator,
		List<GenSource> genSources,
		AtomicInteger countTries,
		AtomicInteger countChecks,
		Consumer<FalsifiedSample> onFalsified,
		Consumer<Sample> onSatisfied,
		Consumer<Throwable> onError
	) {
		try {
			Sample sample = sampleGenerator.generate(genSources);
			countTries.incrementAndGet();
			TryExecutionResult tryResult = tryable.apply(sample);
			if (tryResult.status() != TryExecutionResult.Status.INVALID) {
				countChecks.incrementAndGet();
			}
			if (tryResult.status() == TryExecutionResult.Status.FALSIFIED) {
				FalsifiedSample originalSample = new FalsifiedSample(sample, tryResult.throwable());
				onFalsified.accept(originalSample);
			}
			if (tryResult.status() == TryExecutionResult.Status.SATISFIED) {
				onSatisfied.accept(sample);
			}
		} catch (Throwable t) {
			onError.accept(t);
		}
	}

	private void shrink(FalsifiedSample originalSample, Collection<FalsifiedSample> falsifiedSamples) {
		new FullShrinker(originalSample, tryable).shrinkToEnd(
			falsifiedSamples::add
		);
	}
}
