package jqwik2.api;

import java.util.*;

import jqwik2.internal.*;

public class JqwikProperty {

	private final PropertyRunStrategy strategy;
	private boolean failIfNotSuccessful = false;

	public JqwikProperty() {
		this(PropertyRunStrategy.DEFAULT);
	}

	public JqwikProperty(PropertyRunStrategy strategy) {
		this.strategy = strategy;
	}

	public void failIfNotSuccessful(boolean failIfNotSuccessful) {
		this.failIfNotSuccessful = failIfNotSuccessful;
	}

	public PropertyRunStrategy strategy() {
		return strategy;
	}

	public <T1> PropertyVerifier1<T1> forAll(Arbitrary<T1> arbitrary) {
		return new GenericPropertyVerifier<>(this::buildConfiguration, failIfNotSuccessful, arbitrary);
	}

	public <T1, T2> PropertyVerifier2<T1, T2> forAll(
		Arbitrary<T1> arbitrary1,
		Arbitrary<T2> arbitrary2
	) {
		return new GenericPropertyVerifier<>(this::buildConfiguration, failIfNotSuccessful, arbitrary1, arbitrary2);
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
			case null, default -> throw new IllegalArgumentException("Unsupported generation strategy: " + strategy.generation());
		};
	}

	private boolean isShrinkingEnabled() {
		return strategy.shrinking() == PropertyRunStrategy.Shrinking.FULL;
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
