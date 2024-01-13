package jqwik2.internal;

import java.util.*;

import jqwik2.api.*;
import jqwik2.api.JqwikProperty.*;
import jqwik2.api.support.*;
import org.opentest4j.*;

public class GenericPropertyVerifier<T1, T2>
	implements PropertyVerifier1<T1>, PropertyVerifier2<T1, T2> {

	private final PropertyRunConfiguration configuration;
	private final boolean failIfNotSuccessful;
	private final Arbitrary<T1> arbitrary1;
	private final Arbitrary<T2> arbitrary2;

	public GenericPropertyVerifier(PropertyRunConfiguration configuration, boolean failIfNotSuccessful, Arbitrary<T1> arbitrary) {
		this(configuration, failIfNotSuccessful, arbitrary, null);
	}

	public GenericPropertyVerifier(PropertyRunConfiguration configuration, boolean failIfNotSuccessful, Arbitrary<T1> arbitrary1, Arbitrary<T2> arbitrary2) {
		this.configuration = configuration;
		this.failIfNotSuccessful = failIfNotSuccessful;
		this.arbitrary1 = arbitrary1;
		this.arbitrary2 = arbitrary2;
	}

	@SuppressWarnings("unchecked")
	@Override
	public PropertyRunResult check(C1<T1> checker) {
		return run(
			List.of(arbitrary1.generator()),
			safeTryable(args -> {
				T1 v1 = (T1) args.get(0);
				return checker.check(v1);
			})
		);
	}

	@SuppressWarnings("unchecked")
	@Override
	public PropertyRunResult check(C2<T1, T2> checker) {
		return run(
			List.of(arbitrary1.generator(), arbitrary2.generator()),
			safeTryable(args -> {
				T1 v1 = (T1) args.get(0);
				T2 v2 = (T2) args.get(1);
				return checker.check(v1, v2);
			})
		);
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

	@Override
	public PropertyRunResult verify(V2<T1, T2> verifier) {
		return check(verifier.asCheck());
	}

	private PropertyRunResult run(List<Generator<?>> generators, Tryable tryable) {
		var propertyCase = new PropertyCase(generators, tryable);
		var result = propertyCase.run(configuration);
		return failIfNotSuccessful(result);
	}

	private PropertyRunResult failIfNotSuccessful(PropertyRunResult result) {
		if (!failIfNotSuccessful) {
			return result;
		}
		if (result.isAborted()) {
			var abortionException =
				result.abortionReason()
					  .orElse(new TestAbortedException("Property aborted for unknown reason"));
			ExceptionSupport.throwAsUnchecked(abortionException);
		}
		if (result.isFailed()) {
			var failureException = failureException(result.falsifiedSamples());
			ExceptionSupport.throwAsUnchecked(failureException);
		}
		return result;
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

	@Override
	public PropertyRunResult verify(V1<T1> verifier) {
		return check(verifier.asCheck());
	}

	@FunctionalInterface
	private interface ThrowingTryable {
		boolean apply(List<Object> args) throws Throwable;
	}
}
