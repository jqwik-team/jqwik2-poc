package jqwik2.api.arbitraries;

import jqwik2.api.*;
import jqwik2.internal.generators.*;

public class Values {

	private Values() {}

	public static <T> Arbitrary<T> just(T value) {
		return new Arbitrary<>() {
			@Override
			public Generator<T> generator() {
				return BaseGenerators.just(value);
			}
		};
	}
}
