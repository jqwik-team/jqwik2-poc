package jqwik2;

import java.time.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;
import java.util.function.*;

import jqwik2.api.*;

import static jqwik2.api.PropertyRunResult.Status.*;

public class PropertyCase {
	private final List<Generator<?>> generators;
	private final Tryable tryable;

	private final Duration timeout;
	private final Supplier<ExecutorService> executorServiceSupplier;
	private Consumer<Sample> onSatisfied = ignore -> {};

	public PropertyCase(
		List<Generator<?>> generators, Tryable tryable
	) {
		this(generators, tryable,
			 Duration.ofSeconds(10), () -> Executors.newSingleThreadExecutor()
		);
	}

	public PropertyCase(
		List<Generator<?>> generators, Tryable tryable,
		Duration timeout, Supplier<ExecutorService> executorServiceSupplier
	) {
		this.generators = generators;
		this.tryable = tryable;
		this.timeout = timeout;
		this.executorServiceSupplier = executorServiceSupplier;
	}

	void onSuccessful(Consumer<Sample> onSuccessful) {
		this.onSatisfied = onSuccessful;
	}

	@SuppressWarnings("OverlyLongMethod")
	PropertyRunResult run(PropertyRunConfiguration runConfiguration) {
		if (!(runConfiguration instanceof PropertyRunConfiguration.Randomized)) {
			throw new IllegalArgumentException("Unsupported run configuration: " + runConfiguration);
		}
		RandomGenSource randomGenSource = randomSource((PropertyRunConfiguration.Randomized) runConfiguration);
		int maxTries = runConfiguration.maxTries();
		boolean shrinkingEnabled = runConfiguration.shrinkingEnabled();

		int maxEdgeCases = Math.max(maxTries, 10);
		double edgeCasesProbability = ((PropertyRunConfiguration.Randomized) runConfiguration).edgeCasesProbability();
		List<Generator<Object>> effectiveGenerators = withEdgeCases(edgeCasesProbability, maxEdgeCases);

		SampleGenerator sampleGenerator = new SampleGenerator(effectiveGenerators);

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
			return new PropertyRunResult(SUCCESSFUL, countTries.get(), countChecks.get());
		}

		FalsifiedSample originalSample = falsifiedSamples.first();
		if (shrinkingEnabled) {
			shrink(originalSample, falsifiedSamples);
		}

		return new PropertyRunResult(
			FAILED, countTries.get(), countChecks.get(),
			falsifiedSamples
		);
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

	private static RandomGenSource randomSource(PropertyRunConfiguration.Randomized randomized) {
		return randomized.seed() == null
				   ? new RandomGenSource()
				   : new RandomGenSource(randomized.seed());
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
		new FullShrinker(originalSample, tryable).shrinkToEnd(
			falsifiedSamples::add
		);
	}
}
