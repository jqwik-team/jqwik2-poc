package jqwik2.tdd;

import jqwik2.api.*;
import jqwik2.api.description.*;
import jqwik2.internal.description.*;

class TddPropertyBuilder implements TddProperty.Builder {
	private final String propertyId;

	TddPropertyBuilder() {
		this(defaultId());
	}

	private static String defaultId() {
		StackTraceElement[] stackTrace = new RuntimeException().getStackTrace();
		for (StackTraceElement element : stackTrace) {
			if (element.getClassName().equals(TddPropertyBuilder.class.getName())) {
				continue;
			}
			if (element.getClassName().equals(TDD.class.getName())) {
				continue;
			}
			return element.getClassName() + "#" + element.getMethodName();
		}
		throw new IllegalStateException("Could not determine default id for tdd property");
	}

	TddPropertyBuilder(String propertyId) {
		this.propertyId = propertyId;
	}

	@Override
	public <T1> TddProperty.P1<T1> forAll(Arbitrary<T1> arbitrary) {
		return new TddP1<>(propertyId, arbitrary);
	}

}
