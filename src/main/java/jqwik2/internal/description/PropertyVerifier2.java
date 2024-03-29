package jqwik2.internal.description;

import java.util.*;

import jqwik2.api.*;
import jqwik2.api.description.*;
import jqwik2.api.functions.*;

final class PropertyVerifier2<T1, T2> extends AbstractPropertyVerifier
		implements PropertyDescription.Verifier2<T1, T2> {
	private final Arbitrary<T1> a1;
	private final Arbitrary<T2> a2;

	PropertyVerifier2(
			String propertyId,
			List<Classifier> classifiers,
			Arbitrary<T1> a1, Arbitrary<T2> a2
	) {
		super(propertyId, classifiers);
		this.a1 = a1;
		this.a2 = a2;
	}

	@Override
	public PropertyDescription check(Check.C2<T1, T2> checker) {
		return new GenericPropertyDescription(propertyId, List.of(a1, a2), checker.asCondition(), classifiers);
	}

	@Override
	public PropertyDescription verify(Verify.V2<T1, T2> verifier) {
		return check(verifier.asCheck());
	}

	@Override
	public PropertyDescription.Verifier2<T1, T2> classify(List<Classifier.Case<Check.C2<T1, T2>>> cases) {
		var genericCases = cases.stream()
								.map(c -> (Classifier.Case) c)
								.toList();
		ensureValidCases(genericCases);
		var newClassifiers = new ArrayList<>(classifiers);
		newClassifiers.add(new PropertyClassifier(genericCases));
		return new PropertyVerifier2<>(propertyId, newClassifiers, a1, a2);
	}

	@Override
	public boolean equals(Object obj) {
		if (obj == this) return true;
		if (obj == null || obj.getClass() != this.getClass()) return false;
		var that = (PropertyVerifier2<?, ?>) obj;
		return super.equals(that) &&
				Objects.equals(this.a1, that.a1) &&
				Objects.equals(this.a2, that.a2);
	}

	@Override
	public int hashCode() {
		return Objects.hash(super.hashCode(), a1, a2);
	}
}
