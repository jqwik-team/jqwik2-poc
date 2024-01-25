package jqwik2.api.arbitraries;

import jqwik2.internal.arbitraries.*;

public class Numbers {

	private Numbers() {}

	public static IntegerArbitrary integers() {
		return new DefaultIntegerArbitrary();
	}
}
