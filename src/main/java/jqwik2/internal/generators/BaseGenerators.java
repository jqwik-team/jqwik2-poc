package jqwik2.internal.generators;

import java.util.*;
import java.util.function.*;

import jqwik2.api.*;
import jqwik2.api.arbitraries.*;
import jqwik2.internal.*;

public class BaseGenerators {

	public static final int DEFAULT_COLLECTION_SIZE = 255;

	private BaseGenerators() {}

	public static Generator<Integer> integers(int min, int max) {
		return integers(min, max, RandomChoice.Distribution.UNIFORM);
	}

	public static Generator<Integer> integers(int min, int max, RandomChoice.Distribution distribution) {
		return new IntegerGenerator(min, max, distribution);
	}

	public static <T> Generator<T> just(T value) {
		return create(() -> value);
	}

	public static <T> Generator<T> create(Supplier<T> supplier) {
		return new CreateGenerator<>(supplier);
	}

	public static <T> Generator<T> choose(Collection<? extends T> values) {
		Collection<Pair<Integer, T>> frequencies = values.stream()
														 .map(v -> Pair.of(1, (T) v))
														 .toList();
		return new FrequencyGenerator<>(frequencies);
	}

	public static <T> Generator<T> frequency(Collection<Pair<Integer, T>> frequencies) {
		return new FrequencyGenerator<>(frequencies);
	}

	public static <T> Generator<T> oneOf(Collection<Generator<T>> generators) {
		Collection<Pair<Integer, Generator<T>>> frequencies =
			generators.stream()
					  .map(v -> Pair.of(1, v))
					  .toList();
		return new FrequencyOfGenerator<>(frequencies);
	}

	public static <T> Generator<T> combine(Function<Combinators.Sampler, T> combinator) {
		return new CombineGenerator<>(combinator);
	}

	public static <T> Generator<T> lazy(Supplier<Generator<T>> generatorSupplier) {
		return new LazyGenerator(generatorSupplier);
	}
}
