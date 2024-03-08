package jqwik2.internal;

import java.util.*;

import jqwik2.api.*;

public class PropertyBuilder implements JqwikProperty.Builder {
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
			if (element.getClassName().equals(JqwikProperty.class.getName())) {
				continue;
			}
			return element.getClassName() + "#" + element.getMethodName();
		}
		throw new IllegalStateException("Could not determine default id for property");
	}

	@Override
	public <T1> JqwikProperty.Verifier1<T1> forAll(Arbitrary<T1> arbitrary) {
		return new PropertyVerifier1<>(arbitrary);
	}

	private class PropertyVerifier1<T1> implements JqwikProperty.Verifier1<T1> {
		private final Arbitrary<T1> arbitrary;

		public PropertyVerifier1(Arbitrary<T1> arbitrary) {
			this.arbitrary = arbitrary;
		}

		@Override
		public JqwikProperty check(JqwikProperty.C1<T1> checker) {
			return new GenericJqwikProperty(propertyId, List.of(arbitrary), toCondition(checker));
		}

		@SuppressWarnings("unchecked")
		private JqwikProperty.Condition toCondition(JqwikProperty.C1<T1> checker) {
			return args -> checker.check((T1) args.get(0));
		}

		@Override
		public JqwikProperty verify(JqwikProperty.V1<T1> verifier) {
			return check(verifier.asCheck());
		}
	}
}
