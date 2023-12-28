package jqwik2;

import java.time.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;
import java.util.function.*;

import jqwik2.api.*;

import static jqwik2.api.PropertyExecutionResult.Status.*;

public class PropertyCase {
	private final List<Generator<?>> generators;
	private final Tryable tryable;

	private final String seed;
	private final int maxTries;
	private final double edgeCasesProbability;
	private final boolean shrinkingEnabled;

	private final Duration timeout;
	private final Supplier<ExecutorService> executorServiceSupplier;
	private Consumer<Sample> onSatisfied = ignore -> {};

	public PropertyCase(
			List<Generator<?>> generators, Tryable tryable,
			String seed, int maxTries, double edgeCasesProbability, boolean shrinkingEnabled
	) {
		this(generators, tryable,
			 seed, maxTries, edgeCasesProbability, shrinkingEnabled,
			 Duration.ofSeconds(10), () -> Executors.newSingleThreadExecutor()
		);
	}
	public PropertyCase(
			List<Generator<?>> generators, Tryable tryable,
			String seed, int maxTries, double edgeCasesProbability, boolean shrinkingEnabled,
			Duration timeout, Supplier<ExecutorService> executorServiceSupplier
	) {
		this.generators = generators;
		this.tryable = tryable;
		this.seed = seed;
		this.maxTries = maxTries;
		this.edgeCasesProbability = edgeCasesProbability;
		this.shrinkingEnabled = shrinkingEnabled;
		this.timeout = timeout;
		this.executorServiceSupplier = executorServiceSupplier;
	}

	void onSuccessful(Consumer<Sample> onSuccessful) {
		this.onSatisfied = onSuccessful;
	}

	@SuppressWarnings("OverlyLongMethod")
	PropertyExecutionResult execute() {
		RandomGenSource randomGenSource = new RandomGenSource(seed);
		int maxEdgeCases = Math.max(maxTries, 10);
		SampleGenerator sampleGenerator = new SampleGenerator(generators, edgeCasesProbability, maxEdgeCases);

		AtomicInteger countTries = new AtomicInteger(0);
		AtomicInteger countChecks = new AtomicInteger(0);

		int count = 0;
		SortedSet<FalsifiedSample> falsifiedSamples = Collections.synchronizedSortedSet(new TreeSet<>());


		try (var executorService = executorServiceSupplier.get();) {
			Consumer<FalsifiedSample> onFalsified = sample -> {
				falsifiedSamples.add(sample);
				executorService.shutdownNow();
			};

			while (count < maxTries) {
				count++;
				RandomGenSource tryGenSource = randomGenSource.split();
				try {
					Runnable executeTry = () -> executeTry(
						sampleGenerator, tryGenSource, countTries, countChecks,
						onFalsified, onSatisfied
					);
					executorService.submit(executeTry);
				} catch (RejectedExecutionException ignore) {
					// This can happen when a task is submitted after
					// the executor service has been shut down due to a falsified sample.
				}
			}

			waitForAllTriesToFinishOrAtLeastOneIsFalsified(executorService);
		}

		if (falsifiedSamples.isEmpty()) {
			return new PropertyExecutionResult(SUCCESSFUL, countTries.get(), countChecks.get());
		}

		FalsifiedSample originalSample = falsifiedSamples.first();
		shrink(originalSample, falsifiedSamples);
		return new PropertyExecutionResult(
			FAILED, countTries.get(), countChecks.get(),
			falsifiedSamples
		);
	}

	private void waitForAllTriesToFinishOrAtLeastOneIsFalsified(ExecutorService executorService) {
		boolean timeoutOccurred = false;
		try {
			executorService.shutdown();
			timeoutOccurred = !executorService.awaitTermination(timeout.toMillis(), TimeUnit.MILLISECONDS);
		} catch (InterruptedException e) {
			executorService.shutdownNow();
		}
		if (timeoutOccurred) {
			executorService.shutdownNow();
			throw new PropertyAbortedException("Timeout after " + timeout);
		}
	}

	private void executeTry(
		SampleGenerator sampleGenerator,
		GenSource tryGenSource,
		AtomicInteger countTries,
		AtomicInteger countChecks,
		Consumer<FalsifiedSample> onFalsified,
		Consumer<Sample> onSatisfied
	) {
		Sample sample = sampleGenerator.generate(tryGenSource);
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
	}

	private void shrink(FalsifiedSample originalSample, Collection<FalsifiedSample> falsifiedSamples) {
		if (!shrinkingEnabled) {
			return;
		}
		new FullShrinker(originalSample, tryable).shrinkToEnd(
			falsifiedSamples::add
		);
	}
}
