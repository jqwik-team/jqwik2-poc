package jqwik2.api.arbitraries;

import java.util.*;
import java.util.function.*;

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
		return of(Arrays.asList(values));
	}

	public static <T> Arbitrary<T> of(Collection<T> values) {
		return new CacheableArbitrary<>(values) {
			@Override
			public Generator<T> generator() {
				return BaseGenerators.choose(values);
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

	@SafeVarargs
	public static <T> Arbitrary<T> oneOf(Arbitrary<T>... arbitraries) {
		return oneOf(Arrays.asList(arbitraries));
	}

	@SuppressWarnings("unchecked")
	public static <T> Arbitrary<T> oneOf(Collection<Arbitrary<? extends T>> arbitraries) {
		Collection<Generator<T>> generators =
			arbitraries.stream()
					   .map((Arbitrary<? extends T> arbitrary) -> (Generator<T>) arbitrary.generator())
					   .toList();

		return new CacheableArbitrary<>(arbitraries) {
			@Override
			public Generator<T> generator() {
				return BaseGenerators.oneOf(generators);
			}
		};
	}

	@SuppressWarnings("unchecked")
	public static <T> Arbitrary<T> frequencyOf(Pair<Integer, Arbitrary<? extends T>>... frequencies) {
		return frequencyOf(Arrays.asList(frequencies));
	}

	@SuppressWarnings("unchecked")
	public static <T> Arbitrary<T> frequencyOf(Collection<Pair<Integer, Arbitrary<? extends T>>> frequencies) {
		Collection<Pair<Integer, Generator<T>>> generatorFrequencies =
			frequencies.stream()
					   .map(p -> new Pair<>(p.first(), (Generator<T>) p.second().generator()))
					   .toList();
		return new CacheableArbitrary<>(frequencies) {
			@Override
			public Generator<T> generator() {
				return BaseGenerators.frequencyOf(generatorFrequencies);
			}
		};
	}

	public static <T> Arbitrary<T> lazy(Supplier<Arbitrary<T>> supplier) {
		Supplier<Generator<T>> genSupplier = () -> supplier.get().generator();
		return new CacheableArbitrary<>(supplier) {
			@Override
			public Generator<T> generator() {
				return BaseGenerators.lazy(genSupplier);
			}
		};
	}
}
