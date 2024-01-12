package jqwik2.internal;

import java.util.*;
import java.util.function.*;

import jqwik2.api.*;
import jqwik2.api.JqwikProperty.*;
import jqwik2.api.support.*;
import jqwik2.internal.*;

public class GenericPropertyVerifier<T1> implements PropertyVerifier1<T1> {
	private final Arbitrary<T1> arbitrary;

	public GenericPropertyVerifier(Arbitrary<T1> arbitrary) {
		this.arbitrary = arbitrary;
	}

	@SuppressWarnings("unchecked")
	@Override
	public PropertyRunResult check(C1<T1> checker) {
		Tryable tryable = Tryable.from((Function<List<Object>, Object>) args -> {
			T1 v1 = (T1) args.get(0);
			try {
				return checker.check(v1);
			} catch (Throwable t) {
				ExceptionSupport.rethrowIfBlacklisted(t);
				return ExceptionSupport.throwAsUnchecked(t);
			}
		});
		var propertyCase = new PropertyCase(List.of(arbitrary.generator()), tryable);
		return propertyCase.run(configuration());
	}

	@Override
	public PropertyRunResult verify(V1<T1> checker) {
		return check(checker.asCheck());
	}

	private PropertyRunConfiguration configuration() {
		return PropertyRunConfiguration.randomized(100);
	}
}
