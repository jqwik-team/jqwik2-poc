package jqwik2.internal.description;

import java.util.*;

import jqwik2.api.description.*;

abstract class AbstractPropertyInvariant {
	protected final String propertyId;
	protected final List<Classifier> classifiers;

	protected AbstractPropertyInvariant(
			String propertyId,
			List<Classifier> classifiers
	) {
		this.propertyId = propertyId;
		this.classifiers = classifiers;
	}

	protected static void ensureValidCases(List<Classifier.Case> cases) {
		if (cases.isEmpty()) {
			throw new IllegalArgumentException("At least one case is required");
		}
		if (cases.stream().map(Classifier.Case::label).distinct().count() < cases.size()) {
			throw new IllegalArgumentException("All case labels must be unique");
		}
	}

	@Override
	public boolean equals(Object obj) {
		if (obj == this) return true;
		if (obj == null || obj.getClass() != this.getClass()) return false;
		var that = (AbstractPropertyInvariant) obj;
		return Objects.equals(this.propertyId, that.propertyId) &&
				Objects.equals(this.classifiers, that.classifiers);
	}

	@Override
	public int hashCode() {
		return Objects.hash(propertyId, classifiers);
	}

}
