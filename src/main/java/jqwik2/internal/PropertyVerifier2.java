package jqwik2.internal;

import java.util.*;
import java.util.function.*;

import jqwik2.api.*;

public class PropertyVerifier2<T1, T2> extends AbstractPropertyVerifier implements JqwikProperty.Verifier2<T1, T2> {

	private final Arbitrary<T1> arbitrary1;
	private final Arbitrary<T2> arbitrary2;

	public PropertyVerifier2(
		Function<List<Generator<?>>, PropertyRunConfiguration> supplyConfig,
		Runnable onSuccessful,
		BiConsumer<PropertyRunResult, Throwable> onFailed,
		Consumer<Optional<Throwable>> onAborted,
		List<Generator.DecoratorFunction> decorators,
		Arbitrary<T1> arbitrary1, Arbitrary<T2> arbitrary2
	) {
		super(supplyConfig, onSuccessful, onFailed, onAborted, decorators);
		this.arbitrary1 = arbitrary1;
		this.arbitrary2 = arbitrary2;
	}

	@SuppressWarnings("unchecked")
	@Override
	public PropertyRunResult check(JqwikProperty.C2<T1, T2> checker) {
		return run(
			List.of(arbitrary1.generator(), arbitrary2.generator()),
			safeTryable(args -> {
				T1 v1 = (T1) args.get(0);
				T2 v2 = (T2) args.get(1);
				return checker.check(v1, v2);
			})
		);
	}

	@Override
	public PropertyRunResult verify(JqwikProperty.V2<T1, T2> verifier) {
		return check(verifier.asCheck());
	}

}
