package jqwik2.internal;

import java.util.*;
import java.util.function.*;

import jqwik2.api.*;
import jqwik2.api.statistics.*;
import jqwik2.api.support.*;
import org.opentest4j.*;

class AbstractPropertyVerifier {

	@FunctionalInterface
	protected interface ThrowingTryable {
		boolean apply(List<Object> args) throws Throwable;
	}

	private final BiFunction<List<Generator<?>>, Statistics.Checker, PropertyRunConfiguration> supplyConfig;
	private final Runnable onSuccessful;
	private final BiConsumer<PropertyRunResult, Throwable> onFailed;
	private final Consumer<Optional<Throwable>> onAborted;
	private final List<Generator.DecoratorFunction> decorators;
	private final List<Arbitrary<?>> arbitraries;

	protected AbstractPropertyVerifier(
		BiFunction<List<Generator<?>>, Statistics.Checker, PropertyRunConfiguration> supplyConfig,
		Runnable onSuccessful,
		BiConsumer<PropertyRunResult, Throwable> onFailed,
		Consumer<Optional<Throwable>> onAborted,
		List<Generator.DecoratorFunction> decorators,
		List<Arbitrary<?>> arbitraries
	) {
		this.supplyConfig = supplyConfig;
		this.onSuccessful = onSuccessful;
		this.onFailed = onFailed;
		this.onAborted = onAborted;
		this.decorators = decorators;
		this.arbitraries = arbitraries;
	}

	private Generator<?> decorate(Generator<?> generator) {
		Generator<?> toDecorate = generator;
		for (Generator.DecoratorFunction decorator : decorators) {
			toDecorate = decorator.apply(toDecorate);
		}
		return toDecorate;
	}

	private Tryable safeTryable(ThrowingTryable unsafeTryable) {
		return Tryable.from(args -> {
			try {
				return unsafeTryable.apply(args);
			} catch (Throwable t) {
				ExceptionSupport.rethrowIfBlacklisted(t);
				return ExceptionSupport.throwAsUnchecked(t);
			}
		});
	}

	private PropertyRunResult run(List<Generator<?>> generators, Tryable tryable, Statistics.Checker statisticalCheck) {
		var propertyCase = new PropertyCase(generators, tryable);
		var result = propertyCase.run(supplyConfig.apply(generators, statisticalCheck));
		executeResultCallbacks(result);
		return result;
	}

	private void executeResultCallbacks(PropertyRunResult result) {
		switch (result.status()) {
			case SUCCESSFUL:
				onSuccessful.run();
				break;
			case FAILED:
				onFailed.accept(result, failureException(result.falsifiedSamples()));
				break;
			case ABORTED:
				onAborted.accept(result.abortionReason());
				break;
		}
	}

	private Throwable failureException(SortedSet<FalsifiedSample> falsifiedSamples) {
		if (falsifiedSamples.isEmpty()) {
			return new AssertionFailedError("Property failed but no falsified samples found");
		}
		var smallestSample = falsifiedSamples.getFirst();
		return smallestSample.thrown().orElseGet(
			() -> {
				var message = "Property failed with sample {%s}".formatted(
					smallestSample.values()
				);
				return new AssertionError(message);
			}
		);
	}

	private List<Generator<?>> generators() {
		List<Generator<?>> generators = new ArrayList<>();
		arbitraries.forEach(a -> generators.add(decorate(a.generator())));
		return generators;
	}

	protected PropertyRunResult run(ThrowingTryable unsafeTryable, Statistics.Checker statisticalCheck) {
		return run(
			generators(),
			safeTryable(unsafeTryable),
			statisticalCheck
		);
	}
}
