package jqwik2.api.arbitraries;

public class Numbers {

	private Numbers() {}

	public static IntegerArbitrary integers() {
		return new DefaultIntegerArbitrary();
	}
}
