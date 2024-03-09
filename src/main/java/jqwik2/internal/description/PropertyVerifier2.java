package jqwik2.internal.description;

import java.util.*;

import jqwik2.api.*;
import jqwik2.api.description.*;

record PropertyVerifier2<T1, T2>(
	String propertyId,
	List<Classifier> classifiers,
	Arbitrary<T1> a1, Arbitrary<T2> a2
)
	implements PropertyDescription.Verifier2<T1, T2> {

	@Override
	public PropertyDescription check(PropertyDescription.C2<T1, T2> checker) {
		return new GenericPropertyDescription(propertyId, List.of(a1, a2), checker.asCondition(), classifiers);
	}

	@Override
	public PropertyDescription verify(PropertyDescription.V2<T1, T2> verifier) {
		return check(verifier.asCheck());
	}

	@Override
	public PropertyDescription.Verifier2<T1, T2> classify(List<Classifier.Case<PropertyDescription.C2<T1, T2>>> cases) {
		var genericCases = cases.stream()
								.map(c -> (Classifier.Case) c)
								.toList();
		var newClassifiers = new ArrayList<>(classifiers);
		newClassifiers.add(new PropertyClassifier(genericCases));
		return new PropertyVerifier2<>(propertyId, newClassifiers, a1, a2);
	}
}
