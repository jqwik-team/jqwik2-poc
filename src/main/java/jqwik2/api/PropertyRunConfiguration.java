package jqwik2.api;

import java.time.*;
import java.util.concurrent.*;
import java.util.function.*;

import jqwik2.internal.*;

public interface PropertyRunConfiguration {

	Supplier<ExecutorService> DEFAULT_EXECUTOR_SERVICE_SUPPLIER = Executors::newSingleThreadExecutor;

	Duration maxRuntime();

	int maxTries();

	Supplier<ExecutorService> supplyExecutorService();

	boolean shrinkingEnabled();

	IterableSampleSource source();

	static PropertyRunConfiguration randomized(int maxTries) {
		return randomized(null, maxTries);
	}

	static PropertyRunConfiguration randomized(String seed, int maxTries) {
		return randomized(seed, maxTries, true, Duration.ofMinutes(10), DEFAULT_EXECUTOR_SERVICE_SUPPLIER);
	}

	static PropertyRunConfiguration randomized(
		String seed, int maxTries,
		boolean shrinkingEnabled
	) {
		return randomized(
			seed, maxTries,
			shrinkingEnabled,
			Duration.ofSeconds(10),
			Executors::newSingleThreadExecutor
		);
	}

	static PropertyRunConfiguration randomized(
		String seed, int maxTries,
		boolean shrinkingEnabled,
		Duration maxRuntime,
		Supplier<ExecutorService> supplyExecutorService
	) {
		return new Configuration(
			seed, maxTries,
			shrinkingEnabled,
			maxRuntime,
			supplyExecutorService
		);
	}

}

record Configuration(
	String seed, int maxTries,
	boolean shrinkingEnabled,
	Duration maxRuntime,
	Supplier<ExecutorService> supplyExecutorService
) implements PropertyRunConfiguration {

	@Override
	public IterableSampleSource source() {
		return randomSource(seed);
	}

	private static IterableSampleSource randomSource(String seed) {
		return seed == null
				   ? new RandomGenSource()
				   : new RandomGenSource(seed);
	}

}
