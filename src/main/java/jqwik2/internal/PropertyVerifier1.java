package jqwik2.internal;

import java.util.*;
import java.util.function.*;

import jqwik2.api.*;
import jqwik2.api.statistics.*;

public class PropertyVerifier1<T1> extends AbstractPropertyVerifier implements JqwikProperty.Verifier1<T1> {

	public PropertyVerifier1(
		Function<List<Generator<?>>, PropertyRunConfiguration> supplyConfig,
		Runnable onSuccessful,
		BiConsumer<PropertyRunResult, Throwable> onFailed,
		Consumer<Optional<Throwable>> onAborted,
		List<Generator.DecoratorFunction> decorators,
		Arbitrary<T1> arbitrary
	) {
		super(supplyConfig, onSuccessful, onFailed, onAborted, decorators, List.of(arbitrary));
	}

	@SuppressWarnings("unchecked")
	@Override
	public PropertyRunResult check(JqwikProperty.C1<T1> checker) {
		return run(args -> {
			T1 v1 = (T1) args.get(0);
			return checker.check(v1);
		});
	}

	@Override
	public PropertyRunResult verify(JqwikProperty.V1<T1> verifier) {
		return check(verifier.asCheck());
	}
}
