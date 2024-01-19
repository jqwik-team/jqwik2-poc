package jqwik2.internal.generators;

import java.util.function.*;

import jqwik2.api.*;

public class BaseGenerators {

	public static final int DEFAULT_COLLECTION_SIZE = 255;

	private BaseGenerators() {}

	public static Generator<Integer> integers(int min, int max) {
		return integers(min, max, RandomChoice.Distribution.biased(5));
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
}
