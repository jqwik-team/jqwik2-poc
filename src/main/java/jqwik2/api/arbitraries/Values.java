package jqwik2.api.arbitraries;

import java.util.*;

import jqwik2.api.*;
import jqwik2.internal.generators.*;

public class Values {

	private Values() {}

	public static <T> Arbitrary<T> just(T value) {
		return () -> BaseGenerators.just(value);
	}

	public static <T> Arbitrary<T> of(T... values) {
		return () -> BaseGenerators.choose(Arrays.asList(values));
	}

}
