package jqwik2.tdd;

import jqwik2.api.*;

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

	@Override
	public <T1, T2> TddProperty.P2<T1, T2> forAll(Arbitrary<T1> a1, Arbitrary<T2> a2) {
		return new TddP2<>(propertyId, a1, a2);
	}

}
