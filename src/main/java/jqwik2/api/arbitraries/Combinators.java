package jqwik2.api.arbitraries;

import java.util.*;
import java.util.function.*;

import jqwik2.api.*;
import jqwik2.internal.arbitraries.*;
import jqwik2.internal.generators.*;

public class Combinators {

	public interface Sampler {
		<T> T draw(Arbitrary<T> arbitrary);
	}

	private Combinators() {}

	public static <T> Arbitrary<T> combine(Function<Sampler, T> combinator) {
		return new CacheableArbitrary<>(combinator) {
			@Override
			public Generator<T> generator() {
				return BaseGenerators.combine(combinator);
			}
		};
	}

}
