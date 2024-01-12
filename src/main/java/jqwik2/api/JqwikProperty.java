package jqwik2.api;

import jqwik2.internal.*;

public class JqwikProperty {

	public interface PropertyVerifier1<T1> {
		PropertyRunResult check(C1<T1> checker);

		PropertyRunResult verify(V1<T1> checker);
	}

	public interface C1<T1> {
		boolean check(T1 v1) throws Throwable;
	}

	public interface V1<T1> {
		void verify(T1 v1) throws Throwable;

		default C1<T1> asCheck() {
			return v1 -> {
				verify(v1);
				return true;
			};
		}
	}

	public <T1> PropertyVerifier1<T1> forAll(Arbitrary<T1> arbitrary) {
		return new GenericPropertyVerifier<>(arbitrary);
	}
}
