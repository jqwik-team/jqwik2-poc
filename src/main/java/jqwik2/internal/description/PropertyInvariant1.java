package jqwik2.internal.description;

import java.util.*;

import jqwik2.api.*;
import jqwik2.api.description.*;
import jqwik2.api.functions.*;

final class PropertyInvariant1<T1> extends AbstractPropertyInvariant
	implements PropertyDescription.Invariant1<T1> {
	private final Arbitrary<T1> a1;

	PropertyInvariant1(
		String propertyId,
		List<Classifier> classifiers,
		Arbitrary<T1> a1
	) {
		super(propertyId, classifiers);
		this.a1 = a1;
	}

	@Override
	public PropertyDescription check(Check.C1<T1> checker) {
		return new GenericPropertyDescription(propertyId, List.of(a1), checker.asCondition(), classifiers);
	}

	@Override
	public PropertyDescription verify(Verify.V1<T1> verifier) {
		return check(verifier.asCheck());
	}

	@Override
	public PropertyDescription.Invariant1<T1> classify(List<Classifier.Case<Check.C1<T1>>> cases) {
		List<Classifier> newClassifiers = addClassifierFromCases(cases);
		return new PropertyInvariant1<>(propertyId, newClassifiers, a1);
	}

	@Override
	public boolean equals(Object obj) {
		if (obj == this) return true;
		if (obj == null || obj.getClass() != this.getClass()) return false;
		var that = (PropertyInvariant1<?>) obj;
		return super.equals(that) && Objects.equals(this.a1, that.a1);
	}

	@Override
	public int hashCode() {
		return Objects.hash(super.hashCode(), a1);
	}
}
