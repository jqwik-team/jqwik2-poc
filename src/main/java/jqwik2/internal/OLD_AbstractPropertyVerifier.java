package jqwik2.internal;

import java.util.*;
import java.util.function.*;

import jqwik2.api.*;
import jqwik2.api.support.*;

class OLD_AbstractPropertyVerifier {

	@FunctionalInterface
	protected interface ThrowingTryable {
		boolean apply(List<Object> args) throws Throwable;
	}

	private final Function<List<Generator<?>>, PropertyRunConfiguration> supplyConfig;
	private final Runnable onSuccessful;
	private final Consumer<PropertyRunResult> onFailed;
	private final List<Generator.DecoratorFunction> decorators;
	private final List<Arbitrary<?>> arbitraries;

	protected OLD_AbstractPropertyVerifier(
		Function<List<Generator<?>>, PropertyRunConfiguration> supplyConfig,
		Runnable onSuccessful, Consumer<PropertyRunResult> onFailed,
		List<Generator.DecoratorFunction> decorators,
		List<Arbitrary<?>> arbitraries
	) {
		this.supplyConfig = supplyConfig;
		this.onSuccessful = onSuccessful;
		this.onFailed = onFailed;
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

	private PropertyRunResult run(List<Generator<?>> generators, Tryable tryable) {
		var propertyCase = new PropertyRun(generators, tryable);
		var result = propertyCase.run(supplyConfig.apply(generators));
		executeResultCallbacks(result);
		return result;
	}

	private void executeResultCallbacks(PropertyRunResult result) {
		switch (result.status()) {
			case SUCCESSFUL:
				onSuccessful.run();
				break;
			case FAILED:
				onFailed.accept(result);
				break;
			case ABORTED:
				break;
		}
	}

	private List<Generator<?>> generators() {
		List<Generator<?>> generators = new ArrayList<>();
		arbitraries.forEach(a -> generators.add(decorate(a.generator())));
		return generators;
	}

	protected PropertyRunResult run(ThrowingTryable unsafeTryable) {
		return run(
			generators(),
			safeTryable(unsafeTryable)
		);
	}
}
