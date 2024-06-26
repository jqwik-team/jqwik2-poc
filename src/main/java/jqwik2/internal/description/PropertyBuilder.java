package jqwik2.internal.description;

import java.util.*;

import jqwik2.api.*;
import jqwik2.api.description.*;

public class PropertyBuilder implements PropertyDescription.Builder {
	private final String propertyId;

	public PropertyBuilder(String propertyId) {
		this.propertyId = propertyId;
	}

	public PropertyBuilder() {
		this(defaultId());
	}

	private static String defaultId() {
		StackTraceElement[] stackTrace = new RuntimeException().getStackTrace();
		for (StackTraceElement element : stackTrace) {
			if (element.getClassName().equals(PropertyBuilder.class.getName())) {
				continue;
			}
			if (element.getClassName().equals(PropertyDescription.class.getName())) {
				continue;
			}
			return element.getClassName() + "#" + element.getMethodName();
		}
		throw new IllegalStateException("Could not determine default id for property");
	}

	@Override
	public <T1> PropertyDescription.Invariant1<T1> forAll(Arbitrary<T1> arbitrary) {
		return new PropertyInvariant1<>(propertyId, List.of(), arbitrary);
	}

	@Override
	public <T1, T2> PropertyDescription.Invariant2<T1, T2> forAll(Arbitrary<T1> a1, Arbitrary<T2> a2) {
		return new PropertyInvariant2<>(propertyId, List.of(), a1, a2);
	}

}
