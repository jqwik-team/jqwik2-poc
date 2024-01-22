package jqwik2.api;

import java.util.*;
import java.util.function.*;
import java.util.stream.*;

import jqwik2.api.database.*;
import jqwik2.api.recording.*;
import jqwik2.internal.*;

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
		if (id == null || id.isEmpty()) {
			throw new IllegalArgumentException("id must not be null");
		}
		this.id = id;
		this.strategy = strategy;
		failureDatabase(JqwikDefaults.defaultFailureDatabase());
	}

	public PropertyRunStrategy strategy() {
		return strategy;
	}

	public <T1> PropertyVerifier1<T1> forAll(Arbitrary<T1> arbitrary) {
		return new GenericPropertyVerifier<>(
			this::buildConfiguration, this::onFailed, this::onAborted,
			decorators(), arbitrary
		);
	}

	private List<Generator.DecoratorFunction> decorators() {
		return switch (strategy.edgeCases()) {
			case MIXIN -> List.of(WithEdgeCasesDecorator.function(0.05, 100));
			case OFF -> List.of();
			case null, default -> throw new IllegalArgumentException("Unsupported edge cases mode: " + strategy.edgeCases());
		};
	}

	public <T1, T2> PropertyVerifier2<T1, T2> forAll(
		Arbitrary<T1> arbitrary1,
		Arbitrary<T2> arbitrary2
	) {
		return new GenericPropertyVerifier<>(
			this::buildConfiguration, this::onFailed, this::onAborted,
			decorators(), arbitrary1, arbitrary2
		);
	}

	private void onFailed(PropertyRunResult result, Throwable throwable) {
		onFailureHandlers.forEach(h -> h.accept(result, throwable));
	}

	private void onAborted(Optional<Throwable> abortionReason) {
		onAbortHandlers.forEach(h -> h.accept(abortionReason));
	}

	private PropertyRunConfiguration buildConfiguration(List<Generator<?>> generators) {
		return switch (strategy.generation()) {
			case RANDOMIZED -> PropertyRunConfiguration.randomized(
				strategy.seed().orElseThrow(() -> new IllegalStateException("Randomized Generation requires a seed")),
				strategy.maxTries(),
				isShrinkingEnabled(),
				strategy.maxRuntime(),
				PropertyRunConfiguration.DEFAULT_EXECUTOR_SERVICE_SUPPLIER
			);
			case EXHAUSTIVE -> PropertyRunConfiguration.exhaustive(
				strategy.maxTries(),
				strategy.maxRuntime(),
				PropertyRunConfiguration.DEFAULT_EXECUTOR_SERVICE_SUPPLIER,
				generators
			);
			case SMART -> PropertyRunConfiguration.smart(
				strategy.seed().orElseThrow(() -> new IllegalStateException("Randomized Generation requires a seed")),
				strategy.maxTries(),
				strategy.maxRuntime(),
				isShrinkingEnabled(),
				PropertyRunConfiguration.DEFAULT_EXECUTOR_SERVICE_SUPPLIER,
				generators
			);
			case SAMPLES -> PropertyRunConfiguration.samples(
				strategy.maxRuntime(),
				isShrinkingEnabled(),
				strategy.samples(),
				PropertyRunConfiguration.DEFAULT_EXECUTOR_SERVICE_SUPPLIER
			);
			case null, default -> throw new IllegalArgumentException("Unsupported generation strategy: " + strategy.generation());
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

	public JqwikProperty withGeneration(PropertyRunStrategy.GenerationMode generationMode) {
		PropertyRunStrategy clonedStrategy = PropertyRunStrategy.create(
			strategy.maxTries(),
			strategy.maxRuntime(),
			strategy.seed().orElse(null),
			strategy.samples(),
			strategy.shrinking(),
			generationMode,
			strategy.edgeCases()
		);
		return new JqwikProperty(id, clonedStrategy);
	}

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
		database.saveFailure(id, strategy.seed().orElse(null), sampleRecordings);
	}

	public interface PropertyVerifier1<T1> {
		PropertyRunResult check(C1<T1> checker);

		PropertyRunResult verify(V1<T1> checker);
	}

	public interface PropertyVerifier2<T1, T2> {
		PropertyRunResult check(C2<T1, T2> checker);

		PropertyRunResult verify(V2<T1, T2> checker);
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
