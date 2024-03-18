package jqwik2.internal.description;

import java.util.*;

import jqwik2.api.*;
import jqwik2.api.description.*;

final class PropertyVerifier1<T1> extends AbstractPropertyVerifier
		implements PropertyDescription.Verifier1<T1> {
	private final Arbitrary<T1> a1;

	PropertyVerifier1(
			String propertyId,
			List<Classifier> classifiers,
			Arbitrary<T1> a1
	) {
		super(propertyId, classifiers);
		this.a1 = a1;
	}

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
		ensureValidCases(genericCases);
		PropertyClassifier classifier = new PropertyClassifier(genericCases);
		var newClassifiers = new ArrayList<>(classifiers);
		newClassifiers.add(classifier);
		return new PropertyVerifier1<>(propertyId, newClassifiers, a1);
	}

	@Override
	public boolean equals(Object obj) {
		if (obj == this) return true;
		if (obj == null || obj.getClass() != this.getClass()) return false;
		var that = (PropertyVerifier1) obj;
		return super.equals(that) && Objects.equals(this.a1, that.a1);
	}

	@Override
	public int hashCode() {
		return Objects.hash(super.hashCode(), a1);
	}
}
