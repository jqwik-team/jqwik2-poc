package jqwik2.api;

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
		return new GenericPropertyVerifier<>(configuration(), failIfNotSuccessful, arbitrary);
	}

	public <T1, T2> PropertyVerifier2<T1, T2> forAll(
		Arbitrary<T1> arbitrary1,
		Arbitrary<T2> arbitrary2
	) {
		return new GenericPropertyVerifier<>(configuration(), failIfNotSuccessful, arbitrary1, arbitrary2);
	}

	private PropertyRunConfiguration configuration() {
		if (strategy.generation() != PropertyRunStrategy.Generation.RANDOMIZED)
			throw new IllegalArgumentException("Only randomized generation is supported at the moment");
		return PropertyRunConfiguration.randomized(
			strategy.seed().orElse(null),
			strategy.maxTries(),
			strategy.shrinking() == PropertyRunStrategy.Shrinking.FULL,
			strategy.maxRuntime(),
			PropertyRunConfiguration.DEFAULT_EXECUTOR_SERVICE_SUPPLIER
		);
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
