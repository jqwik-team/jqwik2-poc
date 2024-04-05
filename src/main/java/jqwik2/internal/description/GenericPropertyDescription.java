package jqwik2.internal.description;

import java.util.*;

import jqwik2.api.*;
import jqwik2.api.description.*;
import jqwik2.api.functions.*;

class GenericPropertyDescription implements PropertyDescription {
	private final String propertyId;
	private final List<Arbitrary<?>> arbitraries;
	private final Condition invariant;
	private final List<Classifier> classifiers;

	GenericPropertyDescription(String propertyId, List<Arbitrary<?>> arbitraries, Condition invariant, List<Classifier> classifiers) {
		this.propertyId = propertyId;
		this.arbitraries = arbitraries;
		this.invariant = invariant;
		this.classifiers = classifiers;
	}

	@Override
	public String id() {
		return propertyId;
	}

	@Override
	public List<Arbitrary<?>> arbitraries() {
		return arbitraries;
	}

	@Override
	public Condition invariant() {
		return invariant;
	}

	@Override
	public int arity() {
		return arbitraries.size();
	}

	@Override
	public List<Classifier> classifiers() {
		return classifiers;
	}
}
