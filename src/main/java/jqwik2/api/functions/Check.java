package jqwik2.api.functions;

public interface Check<C extends Check<C>> {
	Condition asCondition();

	interface C1<T1> extends Check<C1<T1>> {
		boolean check(T1 v1) throws Throwable;

		@SuppressWarnings("unchecked")
		default Condition asCondition() {
			return args -> this.check((T1) args.get(0));
		}
	}

	interface C2<T1, T2> extends Check<C2<T1, T2>> {
		boolean check(T1 v1, T2 v2) throws Throwable;

		@SuppressWarnings("unchecked")
		default Condition asCondition() {
			return args -> this.check((T1) args.get(0), (T2) args.get(1));
		}

	}
}
