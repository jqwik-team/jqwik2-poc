package jqwik2.api.arbitraries;

import java.util.*;

import jqwik2.api.*;
import jqwik2.internal.*;
import jqwik2.internal.arbitraries.*;
import jqwik2.internal.generators.*;

public class Values {

	private Values() {}

	public static <T> Arbitrary<T> just(T value) {
		return new CacheableArbitrary<>(value) {
			@Override
			public Generator<T> generator() {
				return BaseGenerators.just(value);
			}
		};
	}

	@SafeVarargs
	public static <T> Arbitrary<T> of(T... values) {
		return new CacheableArbitrary<>((Object[]) values) {
			@Override
			public Generator<T> generator() {
				return BaseGenerators.choose(Arrays.asList(values));
			}
		};
	}

	@SafeVarargs
	public static <T> Arbitrary<T> frequency(Pair<Integer, ? extends T>... frequencies) {
		return new CacheableArbitrary<>((Object[]) frequencies) {
			@Override
			public Generator<T> generator() {
				return BaseGenerators.frequency(Arrays.asList(frequencies));
			}
		};
	}

}
