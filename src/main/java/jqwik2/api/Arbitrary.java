package jqwik2.api;

import jqwik2.api.arbitraries.*;
import jqwik2.internal.*;
import jqwik2.internal.arbitraries.*;

public interface Arbitrary<T> {

	Generator<T> generator();

	default T sample() {
		return generator().generate(new RandomGenSource(RandomChoice.create()));
	}

	default ListArbitrary<T> list() {
		return new DefaultListArbitrary<>(this);
	}

	default SetArbitrary<T> set() {
		return new DefaultSetArbitrary<>(this);
	}
}
