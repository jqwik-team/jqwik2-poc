package jqwik2.internal.description;

import java.util.*;

import jqwik2.api.*;
import jqwik2.api.description.*;

record PropertyVerifier1<T1>(String propertyId, Arbitrary<T1> a1, List<Classifier> classifiers)
	implements PropertyDescription.Verifier1<T1> {

	@Override
	public PropertyDescription check(PropertyDescription.C1<T1> checker) {
		return new GenericPropertyDescription(propertyId, List.of(a1), checker.asCondition(), classifiers);
	}

	@Override
	public PropertyDescription verify(PropertyDescription.V1<T1> verifier) {
		return check(verifier.asCheck());
	}

	@Override
	public PropertyDescription.Verifier1<T1> classify(List<Classifier.Case<PropertyDescription.C1<T1>>> cases) {
		var genericCases = cases.stream()
								.map(c -> (Classifier.Case) c)
								.toList();
		var newClassifiers = new ArrayList<>(classifiers);
		newClassifiers.add(new PropertyClassifier(genericCases));
		return new PropertyVerifier1<>(propertyId, a1, newClassifiers);
	}

}
