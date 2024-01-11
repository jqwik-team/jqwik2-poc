package jqwik2.api;

import jqwik2.internal.*;

public interface Arbitrary<T> {

	Generator<T> generator();

	default T sample() {
		return generator().generate(new RandomGenSource(RandomChoice.create()));
	}
}
