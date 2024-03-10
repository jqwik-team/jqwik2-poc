package jqwik2.api;

import java.util.*;
import java.util.concurrent.*;
import java.util.function.*;
import java.util.stream.*;

import jqwik2.api.database.*;
import jqwik2.api.recording.*;
import jqwik2.api.validation.*;
import jqwik2.internal.*;
import jqwik2.internal.growing.*;

public class OLD_JqwikProperty {

	private final PropertyValidationStrategy strategy;
	private final String id;
	private final List<Consumer<PropertyRunResult>> onFailureHandlers = new ArrayList<>();
	private FailureDatabase database;

	public OLD_JqwikProperty(PropertyValidationStrategy strategy) {
		this(defaultId(), strategy);
	}

	public OLD_JqwikProperty() {
		this(PropertyValidationStrategy.DEFAULT);
	}

	public OLD_JqwikProperty(String myId) {
		this(myId, PropertyValidationStrategy.DEFAULT);
	}

	private static String defaultId() {
		StackTraceElement[] stackTrace = new RuntimeException().getStackTrace();
		for (StackTraceElement element : stackTrace) {
			if (!element.getClassName().equals(OLD_JqwikProperty.class.getName())) {
				return element.getClassName() + "#" + element.getMethodName();
			}
		}
		throw new IllegalStateException("Could not determine default id for property");
	}

	public OLD_JqwikProperty(String id, PropertyValidationStrategy strategy) {
		this(id, strategy, JqwikDefaults.defaultFailureDatabase());
	}

	private OLD_JqwikProperty(String id, PropertyValidationStrategy strategy, FailureDatabase database) {
		if (id == null || id.isEmpty()) {
			throw new IllegalArgumentException("id must not be null");
		}
		this.id = id;
		this.strategy = strategy;
		failureDatabase(database);
	}

	public PropertyValidationStrategy strategy() {
		return strategy;
	}

	public <T1> Verifier1<T1> forAll(Arbitrary<T1> arbitrary) {
		return new OLD_PropertyVerifier1<>(
			this::buildConfiguration, this::onSuccessful, this::onFailed,
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
		return new OLD_PropertyVerifier2<>(
			this::buildConfiguration, this::onSuccessful, this::onFailed,
			decorators(), arbitrary1, arbitrary2
		);
	}

	private void onFailed(PropertyRunResult result) {
		onFailureHandlers.forEach(h -> h.accept(result));
	}

	private void onSuccessful() {
		database.deleteProperty(id);
	}

	private PropertyRunConfiguration buildConfiguration(List<Generator<?>> generators) {
		if (database.hasFailed(id)) {
			return switch (strategy.afterFailure()) {
				case REPLAY -> replayLastRun(generators);
				case SAMPLES_ONLY -> {
					List<SampleRecording> samples = new ArrayList<>(database.loadFailingSamples(id));
					if (samples.isEmpty()) {
						yield replayLastRun(generators);
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
		return buildDefaultConfiguration(generators, strategy.seedSupplier());
	}

	private PropertyRunConfiguration replayLastRun(List<Generator<?>> generators) {
		Supplier<String> seedSupplier = database.loadSeed(id)
												.map(s -> (Supplier<String>) () -> s)
												.orElseGet(strategy::seedSupplier);
		return buildDefaultConfiguration(generators, seedSupplier);
	}

	private PropertyRunConfiguration buildDefaultConfiguration(
		List<Generator<?>> generators, Supplier<String> seedSupplier
	) {
		return switch (strategy.generation()) {
			case RANDOMIZED -> PropertyRunConfiguration.randomized(
				seedSupplier.get(),
				strategy.maxTries(),
				strategy.maxRuntime(), isShrinkingEnabled(),
				strategy.filterOutDuplicateSamples(),
				serviceSupplier()
			);
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
		return strategy.shrinking() == PropertyValidationStrategy.ShrinkingMode.FULL;
	}

	public String id() {
		return id;
	}

	public void failureDatabase(FailureDatabase database) {
		this.database = database;
		if (onFailureHandlers.isEmpty()) {
			onFailureHandlers.addFirst(this::saveFailureToDatabase);
		} else {
			onFailureHandlers.set(0, this::saveFailureToDatabase);
		}
	}

	private void saveFailureToDatabase(PropertyRunResult result) {
		Set<SampleRecording> sampleRecordings = result.falsifiedSamples().stream()
													  .map(s -> s.sample().recording())
													  .collect(Collectors.toSet());
		database.saveFailure(id, result.effectiveSeed().orElse(null), sampleRecordings);
	}

	public interface Verifier1<T1> {
		PropertyRunResult check(C1<T1> checker);

		PropertyRunResult verify(V1<T1> verifier);
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
