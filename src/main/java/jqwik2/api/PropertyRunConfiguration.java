package jqwik2.api;

import java.time.*;
import java.util.concurrent.*;
import java.util.function.*;

import jqwik2.internal.*;

public sealed interface PropertyRunConfiguration {

	Supplier<ExecutorService> DEFAULT_EXECUTOR_SERVICE_SUPPLIER = Executors::newSingleThreadExecutor;

	Duration maxRuntime();

	int maxTries();

	Supplier<ExecutorService> supplyExecutorService();

	boolean shrinkingEnabled();

	static PropertyRunConfiguration randomized(int maxTries) {
		return randomized(null, maxTries);
	}

	static PropertyRunConfiguration randomized(String seed, int maxTries) {
		return randomized(seed, maxTries, true, 0.05, Duration.ofSeconds(10), DEFAULT_EXECUTOR_SERVICE_SUPPLIER);
	}

	static PropertyRunConfiguration randomized(
		String seed, int maxTries,
		boolean shrinkingEnabled, double edgeCasesProbability
	) {
		return randomized(
			seed, maxTries,
			shrinkingEnabled, edgeCasesProbability,
			Duration.ofSeconds(10), Executors::newSingleThreadExecutor
		);
	}

	static PropertyRunConfiguration randomized(
		String seed, int maxTries,
		boolean shrinkingEnabled, double edgeCasesProbability,
		Duration maxRuntime, Supplier<ExecutorService> supplyExecutorService
	) {
		return new Randomized(
			seed, maxTries,
			shrinkingEnabled, edgeCasesProbability,
			maxRuntime, supplyExecutorService
		);
	}

	record Randomized(
		String seed, int maxTries,
		boolean shrinkingEnabled, double edgeCasesProbability,
		Duration maxRuntime, Supplier<ExecutorService> supplyExecutorService
	) implements PropertyRunConfiguration {
	}
}
