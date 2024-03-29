package jqwik2.api.functions;

public interface Verify<V extends Verify<V>> {

	interface V1<T1> extends Verify<V1<T1>> {
		void verify(T1 v1) throws Throwable;

		default Check.C1<T1> asCheck() {
			return v1 -> {
				verify(v1);
				return true;
			};
		}
	}

	interface V2<T1, T2> extends Verify<V2<T1, T2>> {
		void verify(T1 v1, T2 v2) throws Throwable;

		default Check.C2<T1, T2> asCheck() {
			return (v1, v2) -> {
				verify(v1, v2);
				return true;
			};
		}
	}
}
