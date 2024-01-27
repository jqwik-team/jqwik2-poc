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
		List<Pair<Integer, T>> frequencyList = Arrays.stream(frequencies).map(p -> new Pair<>(p.first(), (T) p.second())).toList();
		return new CacheableArbitrary<>(frequencyList) {
			@Override
			public Generator<T> generator() {
				return BaseGenerators.frequency(frequencyList);
			}
		};
	}

}
