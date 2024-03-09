package jqwik2.internal.description;

import java.util.*;

import jqwik2.api.*;
import jqwik2.api.description.*;

record PropertyVerifier2<T1, T2>(
	String propertyId,
	Arbitrary<T1> a1, Arbitrary<T2> a2
)
	implements PropertyDescription.Verifier2<T1, T2> {

	@Override
	public PropertyDescription check(PropertyDescription.C2<T1, T2> checker) {
		return new GenericPropertyDescription(propertyId, List.of(a1, a2), toCondition(checker));
	}

	@SuppressWarnings("unchecked")
	private Condition toCondition(PropertyDescription.C2<T1, T2> checker) {
		return args -> checker.check(
			(T1) args.get(0),
			(T2) args.get(1)
		);
	}

	@Override
	public PropertyDescription verify(PropertyDescription.V2<T1, T2> verifier) {
		return check(verifier.asCheck());
	}
}
