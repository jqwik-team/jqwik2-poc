package jqwik2.internal;

import java.time.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.function.*;

import jqwik2.api.*;

record RunConfigurationRecord(
	String seed, int maxTries, Duration maxRuntime,
	boolean shrinkingEnabled, boolean filterOutDuplicateSamples,
	Supplier<ExecutorService> supplyExecutorService,
	Supplier<IterableSampleSource> supplySource
) implements PropertyRunConfiguration {

	RunConfigurationRecord {
		if (maxTries < 0) {
			throw new IllegalArgumentException("maxTries must not be negative");
		}
		if (maxRuntime.isNegative()) {
			throw new IllegalArgumentException("maxRuntime must not be negative");
		}
	}

	@Override
	public Optional<String> effectiveSeed() {
		return Optional.ofNullable(seed);
	}

	@Override
	public IterableSampleSource source() {
		return supplySource.get();
	}

	@Override
	public Optional<ExecutorService> executorService() {
		if (supplyExecutorService == null) {
			return Optional.empty();
		}
		return Optional.of(supplyExecutorService.get());
	}
}
