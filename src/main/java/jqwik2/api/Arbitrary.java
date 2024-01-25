package jqwik2.api;

import java.util.function.*;

import jqwik2.api.arbitraries.*;
import jqwik2.internal.*;
import jqwik2.internal.arbitraries.*;

public interface Arbitrary<T> {

	Generator<T> generator();

	default T sample() {
		return generator().generate(new RandomGenSource(RandomChoice.create()));
	}

	default T sample(String seed) {
		return generator().generate(new RandomGenSource(RandomChoice.create(seed)));
	}

	default ListArbitrary<T> list() {
		return new DefaultListArbitrary<>(this);
	}

	default SetArbitrary<T> set() {
		return new DefaultSetArbitrary<>(this);
	}

	default <R> Arbitrary<R> map(Function<T, R> mapper) {
		return () -> Arbitrary.this.generator().map(mapper);
	}

	default <R> Arbitrary<R> flatMap(Function<T, Arbitrary<R>> mapper) {
		return () -> Arbitrary.this.generator().flatMap(t -> mapper.apply(t).generator());
	}
}
