package jqwik2.internal;

import java.util.*;
import java.util.function.*;

import jqwik2.api.*;

public class OLD_PropertyVerifier2<T1, T2> extends OLD_AbstractPropertyVerifier implements OLD_JqwikProperty.Verifier2<T1, T2> {

	public OLD_PropertyVerifier2(
		Function<List<Generator<?>>, PropertyRunConfiguration> supplyConfig,
		Runnable onSuccessful,
		Consumer<PropertyRunResult> onFailed,
		List<Generator.DecoratorFunction> decorators,
		Arbitrary<T1> arbitrary1, Arbitrary<T2> arbitrary2
	) {
		super(supplyConfig, onSuccessful, onFailed, decorators, List.of(arbitrary1, arbitrary2));
	}

	@SuppressWarnings("unchecked")
	@Override
	public PropertyRunResult check(OLD_JqwikProperty.C2<T1, T2> checker) {
		return run(args -> {
			T1 v1 = (T1) args.get(0);
			T2 v2 = (T2) args.get(1);
			return checker.check(v1, v2);
		});
	}

	@Override
	public PropertyRunResult verify(OLD_JqwikProperty.V2<T1, T2> verifier) {
		return check(verifier.asCheck());
	}

}
