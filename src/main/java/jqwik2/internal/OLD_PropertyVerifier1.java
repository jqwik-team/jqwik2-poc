package jqwik2.internal;

import java.util.*;
import java.util.function.*;

import jqwik2.api.*;

public class OLD_PropertyVerifier1<T1> extends OLD_AbstractPropertyVerifier implements OLD_JqwikProperty.Verifier1<T1> {

	public OLD_PropertyVerifier1(
		Function<List<Generator<?>>, PropertyRunConfiguration> supplyConfig,
		Runnable onSuccessful,
		Consumer<PropertyRunResult> onFailed,
		List<Generator.DecoratorFunction> decorators,
		Arbitrary<T1> arbitrary
	) {
		super(supplyConfig, onSuccessful, onFailed, decorators, List.of(arbitrary));
	}

	@SuppressWarnings("unchecked")
	@Override
	public PropertyRunResult check(OLD_JqwikProperty.C1<T1> checker) {
		return run(args -> {
			T1 v1 = (T1) args.get(0);
			return checker.check(v1);
		});
	}

	@Override
	public PropertyRunResult verify(OLD_JqwikProperty.V1<T1> verifier) {
		return check(verifier.asCheck());
	}
}
