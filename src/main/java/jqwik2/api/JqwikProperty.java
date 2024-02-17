package jqwik2.api;

import java.time.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.function.*;
import java.util.stream.*;

import jqwik2.api.database.*;
import jqwik2.api.recording.*;
import jqwik2.api.statistics.*;
import jqwik2.internal.*;
import jqwik2.internal.growing.*;

public class JqwikProperty {

	private final PropertyRunStrategy strategy;
	private final String id;
	private final List<BiConsumer<PropertyRunResult, Throwable>> onFailureHandlers = new ArrayList<>();
	private final List<Consumer<Optional<Throwable>>> onAbortHandlers = new ArrayList<>();
	private FailureDatabase database;

	public JqwikProperty(PropertyRunStrategy strategy) {
		this(defaultId(), strategy);
	}

	public JqwikProperty() {
		this(PropertyRunStrategy.DEFAULT);
	}

	public JqwikProperty(String myId) {
		this(myId, PropertyRunStrategy.DEFAULT);
	}

	private static String defaultId() {
		StackTraceElement[] stackTrace = new RuntimeException().getStackTrace();
		for (StackTraceElement element : stackTrace) {
			if (!element.getClassName().equals(JqwikProperty.class.getName())) {
				return element.getClassName() + "#" + element.getMethodName();
			}
		}
		throw new IllegalStateException("Could not determine default id for property");
	}

	public JqwikProperty(String id, PropertyRunStrategy strategy) {
		this(id, strategy, JqwikDefaults.defaultFailureDatabase());
	}

	private JqwikProperty(String id, PropertyRunStrategy strategy, FailureDatabase database) {
		if (id == null || id.isEmpty()) {
			throw new IllegalArgumentException("id must not be null");
		}
		this.id = id;
		this.strategy = strategy;
		failureDatabase(database);
	}

	public PropertyRunStrategy strategy() {
		return strategy;
	}

	public <T1> Verifier1<T1> forAll(Arbitrary<T1> arbitrary) {
		return new PropertyVerifier1<>(
			this::buildConfiguration, this::onSuccessful, this::onFailed, this::onAborted,
			decorators(), arbitrary
		);
	}

	private List<Generator.DecoratorFunction> decorators() {
		return switch (strategy.edgeCases()) {
			// TODO: Calculate edge cases probability and maxEdgeCases from maxTries
			case MIXIN -> List.of(WithEdgeCasesDecorator.function(0.05, 100));
			case OFF -> List.of();
			case null, default -> throw new IllegalArgumentException("Unsupported edge cases mode: " + strategy.edgeCases());
		};
	}

	public <T1, T2> Verifier2<T1, T2> forAll(
		Arbitrary<T1> arbitrary1,
		Arbitrary<T2> arbitrary2
	) {
		return new PropertyVerifier2<>(
			this::buildConfiguration, this::onSuccessful, this::onFailed, this::onAborted,
			decorators(), arbitrary1, arbitrary2
		);
	}

	private void onFailed(PropertyRunResult result, Throwable throwable) {
		onFailureHandlers.forEach(h -> h.accept(result, throwable));
	}

	private void onAborted(Optional<Throwable> abortionReason) {
		onAbortHandlers.forEach(h -> h.accept(abortionReason));
	}

	private void onSuccessful() {
		database.deleteProperty(id);
	}

	private PropertyRunConfiguration buildConfiguration(List<Generator<?>> generators, Checker statisticalCheck) {
		if (database.hasFailed(id)) {
			return switch (strategy.afterFailure()) {
				case REPLAY -> replayLastRun(generators, statisticalCheck);
				case SAMPLES_ONLY -> {
					List<SampleRecording> samples = new ArrayList<>(database.loadFailingSamples(id));
					if (samples.isEmpty()) {
						yield replayLastRun(generators, statisticalCheck);
					}
					Collections.sort(samples); // Sorts from smallest to largest
					yield PropertyRunConfiguration.samples(
						strategy.maxRuntime(),
						isShrinkingEnabled(),
						samples,
						serviceSupplier()
					);
				}
				case null -> throw new IllegalStateException("Property has failed before: " + id);
			};
		}
		return buildDefaultConfiguration(generators, strategy.seedSupplier(), statisticalCheck);
	}

	private PropertyRunConfiguration replayLastRun(List<Generator<?>> generators, Checker statisticalCheck) {
		Supplier<String> seedSupplier = database.loadSeed(id)
												.map(s -> (Supplier<String>) () -> s)
												.orElseGet(strategy::seedSupplier);
		return buildDefaultConfiguration(generators, seedSupplier, statisticalCheck);
	}

	private PropertyRunConfiguration buildDefaultConfiguration(
		List<Generator<?>> generators, Supplier<String> seedSupplier, Checker statisticalCheck
	) {
		return switch (strategy.generation()) {
			case RANDOMIZED -> {
				if (statisticalCheck == null) {
					yield PropertyRunConfiguration.randomized(
						seedSupplier.get(),
						strategy.maxTries(),
						strategy.maxRuntime(), isShrinkingEnabled(),
						strategy.filterOutDuplicateSamples(),
						serviceSupplier()
					);
				} else {
					yield PropertyRunConfiguration.randomizedGuided(
						statisticalCheck::guideWith,
						seedSupplier.get(),
						strategy.maxTries(),
						isShrinkingEnabled(),
						strategy.maxRuntime(),
						serviceSupplier()
					);
				}
			}
			case EXHAUSTIVE -> PropertyRunConfiguration.exhaustive(
				strategy.maxTries(),
				strategy.maxRuntime(),
				serviceSupplier(),
				generators
			);
			case SMART -> PropertyRunConfiguration.smart(
				seedSupplier.get(),
				strategy.maxTries(),
				strategy.maxRuntime(),
				isShrinkingEnabled(),
				strategy.filterOutDuplicateSamples(),
				serviceSupplier(),
				generators
			);
			case SAMPLES -> PropertyRunConfiguration.samples(
				strategy.maxRuntime(),
				isShrinkingEnabled(),
				strategy.samples(),
				serviceSupplier()
			);
			case GROWING -> PropertyRunConfiguration.guided(
				GrowingSampleSource::new,
				strategy.maxTries(), strategy.maxRuntime(),
				false, true,
				serviceSupplier()
			);
			case null -> throw new IllegalArgumentException("Unsupported generation strategy: " + strategy.generation());
		};
	}

	private Supplier<ExecutorService> serviceSupplier() {
		return switch (strategy.concurrency()) {
			case SINGLE_THREAD -> null;
			case CACHED_THREAD_POOL -> Executors::newCachedThreadPool;
			case FIXED_THREAD_POOL -> () -> Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());
			case VIRTUAL_THREADS -> Executors::newVirtualThreadPerTaskExecutor;
			case null -> throw new IllegalArgumentException("Unsupported concurrency mode: " + strategy.concurrency());
		};
	}

	private boolean isShrinkingEnabled() {
		return strategy.shrinking() == PropertyRunStrategy.ShrinkingMode.FULL;
	}

	public String id() {
		return id;
	}

	public void onFailed(BiConsumer<PropertyRunResult, Throwable> onFailureHandler) {
		if (onFailureHandlers.contains(onFailureHandler)) {
			return;
		}
		onFailureHandlers.add(onFailureHandler);
	}

	public void onAbort(Consumer<Optional<Throwable>> onAbortHandler) {
		if (onAbortHandlers.contains(onAbortHandler)) {
			return;
		}
		onAbortHandlers.add(onAbortHandler);
	}

	public JqwikProperty withMaxTries(int maxTries) {
		PropertyRunStrategy clonedStrategy = PropertyRunStrategy.create(
			maxTries,
			strategy.maxRuntime(),
			strategy.filterOutDuplicateSamples(),
			strategy.seedSupplier(),
			strategy.samples(),
			strategy.shrinking(),
			strategy.generation(),
			strategy.edgeCases(),
			strategy.afterFailure(),
			strategy.concurrency()
		);
		return new JqwikProperty(id, clonedStrategy, database);
	}

	public JqwikProperty withMaxRuntime(Duration maxRuntime) {
		PropertyRunStrategy clonedStrategy = PropertyRunStrategy.create(
			strategy.maxTries(),
			maxRuntime,
			strategy.filterOutDuplicateSamples(),
			strategy.seedSupplier(),
			strategy.samples(),
			strategy.shrinking(),
			strategy.generation(),
			strategy.edgeCases(),
			strategy.afterFailure(),
			strategy.concurrency()
		);
		return new JqwikProperty(id, clonedStrategy, database);
	}

	public JqwikProperty withGeneration(PropertyRunStrategy.GenerationMode generationMode) {
		PropertyRunStrategy clonedStrategy = PropertyRunStrategy.create(
			strategy.maxTries(),
			strategy.maxRuntime(),
			strategy.filterOutDuplicateSamples(),
			strategy.seedSupplier(),
			strategy.samples(),
			strategy.shrinking(),
			generationMode,
			strategy.edgeCases(),
			strategy.afterFailure(),
			strategy.concurrency()
		);
		return new JqwikProperty(id, clonedStrategy, database);
	}

	public JqwikProperty withAfterFailure(PropertyRunStrategy.AfterFailureMode afterFailureMode) {
		PropertyRunStrategy clonedStrategy = PropertyRunStrategy.create(
			strategy.maxTries(),
			strategy.maxRuntime(),
			strategy.filterOutDuplicateSamples(),
			strategy.seedSupplier(),
			strategy.samples(),
			strategy.shrinking(),
			strategy.generation(),
			strategy.edgeCases(),
			afterFailureMode,
			strategy.concurrency()
		);
		return new JqwikProperty(id, clonedStrategy, database);
	}

	public JqwikProperty withConcurrency(PropertyRunStrategy.ConcurrencyMode concurrencyMode) {
		PropertyRunStrategy clonedStrategy = PropertyRunStrategy.create(
			strategy.maxTries(),
			strategy.maxRuntime(),
			strategy.filterOutDuplicateSamples(),
			strategy.seedSupplier(),
			strategy.samples(),
			strategy.shrinking(),
			strategy.generation(),
			strategy.edgeCases(),
			strategy.afterFailure(),
			concurrencyMode
		);
		return new JqwikProperty(id, clonedStrategy, database);
	}

	public JqwikProperty withEdgeCases(PropertyRunStrategy.EdgeCasesMode edgeCasesMode) {
		PropertyRunStrategy clonedStrategy = PropertyRunStrategy.create(
			strategy.maxTries(),
			strategy.maxRuntime(),
			strategy.filterOutDuplicateSamples(),
			strategy.seedSupplier(),
			strategy.samples(),
			strategy.shrinking(),
			strategy.generation(),
			edgeCasesMode,
			strategy.afterFailure(),
			strategy.concurrency()
		);
		return new JqwikProperty(id, clonedStrategy, database);	}


	public void failureDatabase(FailureDatabase database) {
		this.database = database;
		if (onFailureHandlers.isEmpty()) {
			onFailureHandlers.addFirst((PropertyRunResult result, Throwable throwable) -> saveFailureToDatabase(result));
		} else {
			onFailureHandlers.set(0, (PropertyRunResult result, Throwable throwable) -> saveFailureToDatabase(result));
		}
	}

	private void saveFailureToDatabase(PropertyRunResult result) {
		Set<SampleRecording> sampleRecordings = result.falsifiedSamples().stream()
													  .map(s -> s.sample().recording())
													  .collect(Collectors.toSet());
		database.saveFailure(id, result.effectiveSeed().orElse(null), sampleRecordings);
	}

	public interface Verifier1<T1> {
		default PropertyRunResult check(C1<T1> checker) {
			return check(checker, null);
		}

		default PropertyRunResult verify(V1<T1> verifier) {
			return verify(verifier, null);
		}

		PropertyRunResult check(C1<T1> checker, Checker statisticalCheck);

		PropertyRunResult verify(V1<T1> verifier, Checker statisticalCheck);
	}

	public interface Verifier2<T1, T2> {
		PropertyRunResult check(C2<T1, T2> checker);

		PropertyRunResult verify(V2<T1, T2> verifier);
	}

	public interface C1<T1> {
		boolean check(T1 v1) throws Throwable;
	}

	public interface V1<T1> {
		void verify(T1 v1) throws Throwable;

		default C1<T1> asCheck() {
			return v1 -> {
				verify(v1);
				return true;
			};
		}
	}

	public interface C2<T1, T2> {
		boolean check(T1 v1, T2 v2) throws Throwable;
	}

	public interface V2<T1, T2> {
		void verify(T1 v1, T2 v2) throws Throwable;

		default C2<T1, T2> asCheck() {
			return (v1, v2) -> {
				verify(v1, v2);
				return true;
			};
		}
	}
}
