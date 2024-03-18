package jqwik2.internal.description;

import java.util.*;

import jqwik2.api.*;
import jqwik2.api.description.*;

abstract class AbstractPropertyVerifier {
	protected final String propertyId;
	protected final List<Classifier> classifiers;

	protected AbstractPropertyVerifier(
			String propertyId,
			List<Classifier> classifiers
	) {
		this.propertyId = propertyId;
		this.classifiers = classifiers;
	}

	@Override
	public boolean equals(Object obj) {
		if (obj == this) return true;
		if (obj == null || obj.getClass() != this.getClass()) return false;
		var that = (AbstractPropertyVerifier) obj;
		return Objects.equals(this.propertyId, that.propertyId) &&
				Objects.equals(this.classifiers, that.classifiers);
	}

	@Override
	public int hashCode() {
		return Objects.hash(propertyId, classifiers);
	}

}
