package jqwik2.api;

import java.util.function.*;
import java.util.stream.*;

import jqwik2.api.arbitraries.*;
import jqwik2.internal.*;
import jqwik2.internal.arbitraries.*;

public interface Arbitrary<T> {

	Generator<T> generator();

	default T sample() {
		return sample(false);
	}

	default T sample(boolean withEdgeCases) {
		return samples(withEdgeCases).findFirst().orElseThrow();
	}

	default Stream<T> samples(boolean withEdgeCases) {
		var generator = generator();
		if (withEdgeCases) {
			generator = WithEdgeCasesDecorator.decorate(generator, 0.05, 100);
		}
		var g = generator;
		var source = new RandomGenSource(RandomChoice.create());
		return Stream.iterate(g.generate(source), t -> g.generate(source));
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
