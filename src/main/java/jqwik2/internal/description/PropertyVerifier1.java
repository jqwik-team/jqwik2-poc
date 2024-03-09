package jqwik2.internal.description;

import java.util.*;

import jqwik2.api.*;
import jqwik2.api.description.*;

record PropertyVerifier1<T1>(String propertyId, Arbitrary<T1> a1)
	implements PropertyDescription.Verifier1<T1> {

	@Override
	public PropertyDescription check(PropertyDescription.C1<T1> checker) {
		return new GenericPropertyDescription(propertyId, List.of(a1), toCondition(checker));
	}

	@SuppressWarnings("unchecked")
	private Condition toCondition(PropertyDescription.C1<T1> checker) {
		return args -> checker.check((T1) args.get(0));
	}

	@Override
	public PropertyDescription verify(PropertyDescription.V1<T1> verifier) {
		return check(verifier.asCheck());
	}
}
