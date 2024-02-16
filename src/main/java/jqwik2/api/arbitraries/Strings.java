package jqwik2.api.arbitraries;

import jqwik2.internal.arbitraries.*;

public class Strings {

	private Strings() {}

	public static StringArbitrary strings() {
		return new DefaultStringArbitrary();
	}
}
