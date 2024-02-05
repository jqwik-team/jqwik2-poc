package jqwik2.internal;

import java.time.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.function.*;

import jqwik2.api.*;

record RunConfigurationRecord(
	String seed, int maxTries,
	boolean shrinkingEnabled,
	Duration maxRuntime,
	boolean filterOutDuplicateSamples,
	Supplier<ExecutorService> supplyExecutorService,
	Supplier<IterableSampleSource> supplySource
) implements PropertyRunConfiguration {
	@Override
	public Optional<String> effectiveSeed() {
		return Optional.ofNullable(seed);
	}

	@Override
	public IterableSampleSource source() {
		return supplySource.get();
	}
}
