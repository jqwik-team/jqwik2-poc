package jqwik2.internal;

import java.util.*;

import jqwik2.api.*;

class GenericJqwikProperty implements JqwikProperty {
	private final String propertyId;
	private final List<Arbitrary<?>> arbitraries;
	private final Condition condition;

	GenericJqwikProperty(String propertyId, List<Arbitrary<?>> arbitraries, Condition condition) {
		this.propertyId = propertyId;
		this.arbitraries = arbitraries;
		this.condition = condition;
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
	public Condition condition() {
		return condition;
	}

	@Override
	public int arity() {
		return arbitraries.size();
	}
}
